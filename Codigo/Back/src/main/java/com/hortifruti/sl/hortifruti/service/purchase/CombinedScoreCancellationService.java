package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.exception.purchase.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.model.purchase.Status;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceCancelService;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceService;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Cancela um agrupamento (CombinedScore) em cascata: cancela primeiro a NF-e/boleto já emitidos
 * externamente (Focus NFe / Sicoob, reaproveitando os fluxos existentes) e só então apaga tudo
 * localmente. Vive fora de {@link CombinedScoreService} para evitar dependência circular, já que
 * {@link InvoiceService}/{@link BilletService} já dependem dele.
 *
 * <p>Cada etapa externa roda na sua própria transação (não há um {@code @Transactional} envolvendo
 * o fluxo inteiro): se o cancelamento do boleto falhar depois do cancelamento da NF-e já ter sido
 * confirmado na Sefaz, um rollback não pode desfazer esse cancelamento externo (irreversível) — por
 * isso o estado intermediário fica registrado permanentemente em vez de ser revertido.
 */
@Slf4j
@Service
@AllArgsConstructor
public class CombinedScoreCancellationService {

  private final CombinedScoreService combinedScoreService;
  private final InvoiceService invoiceService;
  private final BilletService billetService;
  private final CombinedScoreHardDeleteService combinedScoreHardDeleteService;

  public void cancelGrouping(Long id) {
    CombinedScore combinedScore = combinedScoreService.findById(id);

    if (combinedScore.getStatus() == Status.PAGO) {
      throw new CombinedScoreException(
          "Não é possível cancelar: o pagamento deste agrupamento (id "
              + id
              + ") já foi"
              + " confirmado.");
    }

    if (combinedScore.isHasInvoice()) {
      cancelInvoiceOrThrow(combinedScore.getInvoiceRef());
    }

    // Releitura: o passo acima já comitou numa transação própria e pode ter mudado o estado local
    // — nunca confiar no snapshot lido antes da chamada externa.
    CombinedScore afterInvoiceStep = combinedScoreService.findById(id);

    if (afterInvoiceStep.isHasBillet()) {
      cancelBilletOrThrow(id);
    }

    combinedScoreHardDeleteService.hardDeleteLocally(id);
  }

  private void cancelInvoiceOrThrow(String ref) {
    if (ref == null || ref.isBlank()) {
      log.warn(
          "CombinedScore marcado com hasInvoice=true mas sem invoiceRef — pulando cancelamento"
              + " externo da NF-e.");
      return;
    }
    try {
      invoiceService.cancelInvoice(ref, InvoiceCancelService.MANUAL_CANCEL_JUSTIFICATIVA, true);
    } catch (InvoiceException e) {
      throw new CombinedScoreException(
          "Não é possível cancelar o agrupamento: a nota fiscal (ref "
              + ref
              + ") não pôde ser cancelada na Sefaz ("
              + e.getMessage()
              + "). O prazo de cancelamento pode ter expirado, ou a nota já pode ter sido usada em"
              + " escrituração — considere emitir uma carta de correção ou uma nota de devolução.",
          e);
    }
  }

  private void cancelBilletOrThrow(Long combinedScoreId) {
    try {
      ResponseEntity<String> response = billetService.cancelBillet(combinedScoreId);
      if (response.getStatusCode() == HttpStatus.CONFLICT) {
        throw new CombinedScoreException(
            "Não é possível cancelar o agrupamento: o boleto já está em processo de"
                + " baixa/liquidação no Sicoob e não pode mais ser cancelado.");
      }
    } catch (BilletException | IOException e) {
      throw new CombinedScoreException(
          "Não é possível cancelar o agrupamento: o boleto não pôde ser baixado no Sicoob ("
              + e.getMessage()
              + ").",
          e);
    }
  }
}
