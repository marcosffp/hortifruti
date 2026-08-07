package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Job assíncrono disparado logo após a emissão da NF: faz polling do status na Focus NFe até ser
 * autorizada (ou rejeitada/cancelada/com erro), baixa XML/DANFE e persiste via {@link
 * FiscalNoteXmlStorageStore}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class FiscalNoteIssuancePoller {

  private static final int MAX_POLL_ATTEMPTS = 36;
  private static final long POLL_INTERVAL_MS = 10_000;

  private final FiscalNoteFocusNfeClient focusNfeClient;
  private final FiscalNoteXmlStorageStore store;
  private final CombinedScoreService combinedScoreService;

  @Async
  void triggerSaveAfterIssuance(String ref) {
    try {
      for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
        Thread.sleep(POLL_INTERVAL_MS);

        JsonNode rootNode = focusNfeClient.fetchStatus(ref);
        String status = rootNode.path("status").asText();

        if (status.contains("autorizado")) {
          String xmlPath = rootNode.path("caminho_xml_nota_fiscal").asText();
          if (xmlPath == null || xmlPath.isBlank()) {
            log.warn("[FiscalNoteXmlStorage] XML path vazio para ref={}, aguardando...", ref);
            continue;
          }

          byte[] xmlBytes = focusNfeClient.downloadFileBytes(xmlPath, MediaType.APPLICATION_XML);
          if (xmlBytes == null || xmlBytes.length == 0) {
            log.warn("[FiscalNoteXmlStorage] XML bytes vazios para ref={}", ref);
            continue;
          }

          byte[] danfeBytes = focusNfeClient.downloadDanfeBytesBestEffort(rootNode, ref);

          NfMetadata metadata = focusNfeClient.extractMetadata(rootNode, ref);
          store.persistIfAbsent(ref, new String(xmlBytes), danfeBytes, metadata);
          return;
        }

        if (status.contains("cancelado")
            || status.contains("denegado")
            || status.contains("erro")) {
          log.warn(
              "[FiscalNoteXmlStorage] NF ref={} com status={}, salvamento cancelado", ref, status);
          revertInvoiceOnRejection(ref, status);
          return;
        }
      }

      log.error(
          "[FiscalNoteXmlStorage] Timeout: NF ref={} não foi autorizada após {} tentativas",
          ref,
          MAX_POLL_ATTEMPTS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("[FiscalNoteXmlStorage] Thread interrompida ao salvar ref={}", ref, e);
    } catch (Exception e) {
      log.error(
          "[FiscalNoteXmlStorage] Erro ao salvar XML para ref={}: {}", ref, e.getMessage(), e);
    }
  }

  /**
   * A Sefaz rejeitou/cancelou/denegou a NF depois que {@code IssueInvoice} já tinha marcado {@code
   * hasInvoice=true} otimisticamente (a Focus NFe respondeu "processando_autorizacao" na hora da
   * emissão). Sem reverter aqui, o agrupamento fica travado mostrando "Ver NF" no front para uma
   * nota que nunca foi autorizada. Best-effort: uma falha aqui não deve derrubar o job de polling,
   * só fica registrada para investigação manual.
   */
  private void revertInvoiceOnRejection(String ref, String status) {
    try {
      combinedScoreService.clearInvoiceAfterRejection(ref);
    } catch (Exception e) {
      log.error(
          "[FiscalNoteXmlStorage] Falha ao reverter hasInvoice do agrupamento para ref={}"
              + " (status={}): {}",
          ref,
          status,
          e.getMessage(),
          e);
    }
  }
}
