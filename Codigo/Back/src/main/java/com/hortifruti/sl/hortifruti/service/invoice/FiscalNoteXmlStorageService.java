package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.dto.invoice.FiscalNoteXmlStorageResponse;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Fachada de acesso ao XML/DANFE persistido de cada NF-e, para o restante do módulo de invoice.
 * Delega para três colaboradores infra dedicados, cada um com uma responsabilidade só: {@link
 * FiscalNoteFocusNfeClient} (comunicação HTTP com a Focus NFe), {@link FiscalNoteXmlStorageStore}
 * (upload/leitura no R2 + linha no banco, com locking por ref via {@link FiscalNoteRefLock}) e
 * {@link FiscalNoteIssuancePoller} (job assíncrono de polling pós-emissão, incluindo a reversão de
 * {@code hasInvoice} quando a NF é rejeitada).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalNoteXmlStorageService {

  private final FiscalNoteIssuancePoller poller;
  private final FiscalNoteXmlStorageStore store;
  private final FiscalNoteFocusNfeClient focusNfeClient;
  private final CombinedScoreService combinedScoreService;

  /** Triggered async right after invoice issuance. Polls until authorized, then saves XML. */
  public void triggerSaveAfterIssuance(String ref) {
    poller.triggerSaveAfterIssuance(ref);
  }

  /**
   * Called from downloadXml as a safety net — saves if not yet persisted.
   *
   * <p>Roda em transação própria (REQUIRES_NEW): pode ser chamado ao mesmo tempo que o job
   * assíncrono de polling ({@link FiscalNoteIssuancePoller}) para a mesma ref; {@link
   * FiscalNoteXmlStorageStore} serializa esse trecho por ref e desfaz o upload no R2 se ainda assim
   * perder a corrida, então isso não gera arquivo duplicado. Uma falha aqui é só best-effort — não
   * pode envenenar a transação de quem chamou (ex: downloadXml/downloadDanfe), senão o commit do
   * chamador falha com UnexpectedRollbackException mesmo com a exceção sendo capturada abaixo.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void saveIfAbsent(String ref, byte[] xmlBytes) {
    if (store.hasXmlContent(ref)) return;
    try {
      JsonNode rootNode = focusNfeClient.fetchStatus(ref);
      NfMetadata metadata = focusNfeClient.extractMetadata(rootNode, ref);
      byte[] danfeBytes = focusNfeClient.downloadDanfeBytesBestEffort(rootNode, ref);
      store.persistIfAbsent(ref, new String(xmlBytes), danfeBytes, metadata);
    } catch (Exception e) {
      log.warn(
          "[FiscalNoteXmlStorage] Não foi possível salvar XML como backup para ref={}: {}",
          ref,
          e.getMessage());
    }
  }

  /**
   * Called from DanfeXmlService.downloadDanfe as a safety net — saves the DANFE if not yet
   * persisted. Unlike {@link #saveIfAbsent}, this only touches the {@code danfeObjectKey} field:
   * the row may already exist (XML saved earlier) or not (DANFE requested before XML was ever
   * persisted).
   *
   * <p>Roda em transação própria (REQUIRES_NEW) pelo mesmo motivo de {@link #saveIfAbsent}.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void saveDanfeIfAbsent(String ref, byte[] danfeBytes) {
    store.saveDanfeIfAbsent(ref, danfeBytes);
  }

  /**
   * Reconcilia com a Focus NFe antes de listar: o job assíncrono de emissão ({@link
   * FiscalNoteIssuancePoller}) roda só em memória, então uma NF pode ter sido autorizada na Sefaz
   * (e por isso aparecer, por exemplo, no relatório "Relação de Vendas", que consulta a Focus NFe
   * ao vivo por {@code CombinedScore}) sem que o XML jamais tenha sido persistido aqui — ex.: a
   * aplicação reiniciou no meio da janela de polling. Sem essa reconciliação, a NF fica "invisível"
   * pra sempre nesta listagem e no ZIP de exportação mensal (que usa a mesma consulta), mesmo já
   * emitida de verdade. {@link #ensureSaved} é best-effort e barato quando não há nada pra corrigir
   * ({@link FiscalNoteXmlStorageStore#hasXmlContent} evita a chamada HTTP nesse caso).
   */
  @Transactional
  public List<FiscalNoteXmlStorageResponse> findByPeriod(LocalDate startDate, LocalDate endDate) {
    reconcileMissingXmls(startDate, endDate);
    return store.findByPeriod(startDate, endDate);
  }

  private void reconcileMissingXmls(LocalDate startDate, LocalDate endDate) {
    for (CombinedScore combinedScore :
        combinedScoreService.getCombinedScoresWithInvoice(startDate, endDate)) {
      String ref = combinedScore.getInvoiceRef();
      if (ref != null && !ref.isBlank()) {
        ensureSaved(ref);
      }
    }
  }

  /**
   * Garante que o XML dessa ref esteja persistido, buscando ao vivo na Focus NFe se ainda faltar.
   * Best-effort: uma falha aqui (nota ainda processando, erro de rede, etc.) não pode propagar — é
   * chamado num loop de reconciliação onde uma ref problemática não pode impedir as demais.
   *
   * <p>Roda em transação própria (REQUIRES_NEW) pelo mesmo motivo de {@link #saveIfAbsent}.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void ensureSaved(String ref) {
    if (store.hasXmlContent(ref)) return;
    try {
      JsonNode rootNode = focusNfeClient.fetchStatus(ref);
      if (!rootNode.path("status").asText().contains("autorizado")) {
        return;
      }

      String xmlPath = rootNode.path("caminho_xml_nota_fiscal").asText();
      if (xmlPath == null || xmlPath.isBlank()) {
        return;
      }

      byte[] xmlBytes = focusNfeClient.downloadFileBytes(xmlPath, MediaType.APPLICATION_XML);
      if (xmlBytes == null || xmlBytes.length == 0) {
        return;
      }

      byte[] danfeBytes = focusNfeClient.downloadDanfeBytesBestEffort(rootNode, ref);
      NfMetadata metadata = focusNfeClient.extractMetadata(rootNode, ref);
      store.persistIfAbsent(ref, new String(xmlBytes), danfeBytes, metadata);
    } catch (Exception e) {
      log.warn(
          "[FiscalNoteXmlStorage] Não foi possível reconciliar XML faltante para ref={}: {}",
          ref,
          e.getMessage());
    }
  }

  @Transactional
  public byte[] getXmlContent(String ref) {
    return store.getXmlContent(ref);
  }

  /**
   * Retorna o DANFE (PDF) já persistido no R2 para o ref informado, ou {@code null} se ainda não
   * foi salvo — o chamador deve então buscar ao vivo na Focus NFe e salvar via {@link
   * #saveDanfeIfAbsent}.
   */
  @Transactional
  public byte[] getDanfeContent(String ref) {
    return store.getDanfeContent(ref);
  }

  /** Agenda o cancelamento (mover para canceladas/) para depois do commit da transação corrente. */
  public void cancelXmlFileAfterCommit(String ref) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              cancelXmlFile(ref);
            }
          });
    } else {
      cancelXmlFile(ref);
    }
  }

  @Transactional
  public void cancelXmlFile(String ref) {
    store.cancelXmlFile(ref);
  }

  @Transactional
  public String getNfNumber(String ref) {
    return store.getNfNumber(ref);
  }
}
