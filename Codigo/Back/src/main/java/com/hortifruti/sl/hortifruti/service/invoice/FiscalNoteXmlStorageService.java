package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.dto.invoice.FiscalNoteXmlStorageResponse;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    if (store.existsByRef(ref)) return;
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

  @Transactional
  public List<FiscalNoteXmlStorageResponse> findByPeriod(LocalDate startDate, LocalDate endDate) {
    return store.findByPeriod(startDate, endDate);
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
