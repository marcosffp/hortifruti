package com.hortifruti.sl.hortifruti.service.invoice;

import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Atualiza CombinedScore/XML local após um cancelamento de NF-e CONFIRMADO (pela Focus NFe/SEFAZ).
 * Extraído de {@link InvoiceCancelService} para ser compartilhado com {@link
 * FiscalNoteCancellationPoller} sem criar dependência circular entre os dois (o service dispara o
 * poller, então nenhum dos dois pode depender do outro).
 */
@Component
@RequiredArgsConstructor
@Slf4j
class InvoiceCancellationRecordUpdater {

  private final CombinedScoreService combinedScoreService;
  private final FiscalNoteXmlStorageService fiscalNoteXmlStorageService;

  /**
   * Best-effort: usado também pelo cancelamento manual/avulso, quando pode não existir nenhum
   * registro local vinculado à ref informada — nesse caso a NF já foi cancelada de fato na Focus
   * NFe e isso não deve ser reportado como falha.
   */
  void updateLocalRecordsBestEffort(String ref) {
    try {
      if (combinedScoreService.findByInvoiceRef(ref).isEmpty()) {
        log.info(
            "NF-e {} cancelada na Focus NFe sem CombinedScore local correspondente (cancelamento"
                + " avulso).",
            ref);
      } else {
        combinedScoreService.updateStatusAfterInvoiceCancellation(ref);
      }
    } catch (Exception e) {
      log.warn(
          "NF-e {} cancelada na Focus NFe, mas falhou ao atualizar o registro local"
              + " correspondente.",
          ref,
          e);
    }
    try {
      fiscalNoteXmlStorageService.cancelXmlFileAfterCommit(ref);
    } catch (Exception e) {
      log.warn("Não foi possível marcar o XML armazenado da NF-e {} como cancelado.", ref, e);
    }
  }
}
