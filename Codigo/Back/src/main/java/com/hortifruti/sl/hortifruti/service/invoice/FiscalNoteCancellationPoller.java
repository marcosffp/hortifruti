package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Job assíncrono disparado logo após o DELETE de cancelamento na Focus NFe, para os casos em que
 * ela responde de forma assíncrona (status {@code processando_cancelamento} em vez de {@code
 * cancelado} síncrono). Faz polling do status até a SEFAZ confirmar o cancelamento (ou mantê-lo
 * rejeitado, ex.: prazo expirado, nota já usada em escrituração) — só então os registros locais
 * (CombinedScore/XML) são marcados como cancelados.
 *
 * <p>Sem isso, {@link InvoiceCancelService} teria que tratar qualquer 2xx do DELETE como
 * cancelamento definitivo, o que reporta "sucesso" ao usuário mesmo quando a SEFAZ ainda não
 * confirmou (ou vai recusar) o cancelamento — o registro local fica cancelado indevidamente
 * enquanto a nota continua válida no ambiente da Focus NFe/SEFAZ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class FiscalNoteCancellationPoller {

  private static final int MAX_POLL_ATTEMPTS = 18;
  private static final long POLL_INTERVAL_MS = 10_000;

  private final FiscalNoteFocusNfeClient focusNfeClient;
  private final InvoiceCancellationRecordUpdater recordUpdater;

  @Async
  void triggerConfirmationAfterCancel(String ref) {
    try {
      for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
        Thread.sleep(POLL_INTERVAL_MS);

        JsonNode rootNode;
        try {
          rootNode = focusNfeClient.fetchStatus(ref);
        } catch (Exception fetchError) {
          log.warn(
              "[FiscalNoteCancellation] Falha ao consultar status da ref={} (tentativa {}/{}): {}",
              ref,
              attempt,
              MAX_POLL_ATTEMPTS,
              fetchError.getMessage());
          continue;
        }

        String status = rootNode.path("status").asText();

        if (status.contains("cancelado")) {
          recordUpdater.updateLocalRecordsBestEffort(ref);
          log.info(
              "[FiscalNoteCancellation] Cancelamento da NF-e ref={} confirmado pela SEFAZ.", ref);
          return;
        }

        if (!status.contains("processando")) {
          // A SEFAZ não confirmou o cancelamento e a nota não está mais em processamento (ex.:
          // voltou para "autorizado" porque o pedido foi rejeitado) — não marca como cancelada.
          log.warn(
              "[FiscalNoteCancellation] SEFAZ não confirmou o cancelamento da NF-e ref={}"
                  + " (status={}) — pedido de cancelamento não foi aceito, nota permanece como"
                  + " está.",
              ref,
              status);
          return;
        }
      }

      log.error(
          "[FiscalNoteCancellation] Timeout: cancelamento da NF-e ref={} não confirmado após {}"
              + " tentativas — requer verificação manual.",
          ref,
          MAX_POLL_ATTEMPTS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error(
          "[FiscalNoteCancellation] Thread interrompida ao confirmar cancelamento da ref={}",
          ref,
          e);
    } catch (Exception e) {
      log.error(
          "[FiscalNoteCancellation] Erro ao confirmar cancelamento da ref={}: {}",
          ref,
          e.getMessage(),
          e);
    }
  }
}
