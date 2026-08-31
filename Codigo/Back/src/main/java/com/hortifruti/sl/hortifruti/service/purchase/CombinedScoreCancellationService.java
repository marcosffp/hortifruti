package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.exception.purchase.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.billet.BilletFile;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.model.purchase.Status;
import com.hortifruti.sl.hortifruti.repository.billet.BilletFileRepository;
import com.hortifruti.sl.hortifruti.repository.invoice.FiscalNoteXmlStorageRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceCancelService;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceService;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final CombinedScoreRepository combinedScoreRepository;
  private final InvoiceService invoiceService;
  private final BilletService billetService;
  private final BilletFileRepository billetFileRepository;
  private final FiscalNoteXmlStorageRepository fiscalNoteXmlStorageRepository;
  private final PurchaseRepository purchaseRepository;
  private final R2StorageService r2StorageService;

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

    hardDeleteLocally(id);
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

  @Transactional
  public void hardDeleteLocally(Long id) {
    CombinedScore combinedScore = combinedScoreService.findByIdForUpdate(id);

    if (combinedScore.getStatus() == Status.PAGO) {
      throw new CombinedScoreException(
          "Não é possível cancelar: o pagamento deste agrupamento já foi confirmado.");
    }
    if (combinedScore.isHasInvoice() || combinedScore.isHasBillet()) {
      throw new CombinedScoreException(
          "Não é possível excluir o agrupamento: ainda há nota fiscal ou boleto ativos"
              + " vinculados a ele.");
    }

    deleteBilletFiles(id);
    deleteFiscalNoteXml(combinedScore.getInvoiceRef());

    // Desvincula as compras de origem antes de apagar o agrupamento — combinedScoreId não é uma
    // FK real (ver convenção do projeto), então sem isso essas compras ficariam com o campo
    // apontando pra um ID que não existe mais, sem nenhuma forma de saber depois que estão livres
    // pra um novo agrupamento (ver PurchaseRepository#clearCombinedScoreId).
    purchaseRepository.clearCombinedScoreId(id);

    combinedScoreRepository.delete(combinedScore);
  }

  private void deleteBilletFiles(Long combinedScoreId) {
    List<BilletFile> files = billetFileRepository.findByCombinedScoreId(combinedScoreId);
    for (BilletFile file : files) {
      deleteR2Quietly(file.getObjectKey());
    }
    billetFileRepository.deleteAll(files);
  }

  private void deleteFiscalNoteXml(String invoiceRef) {
    if (invoiceRef == null || invoiceRef.isBlank()) {
      return;
    }
    fiscalNoteXmlStorageRepository
        .findByRef(invoiceRef)
        .ifPresent(
            storage -> {
              deleteR2Quietly(storage.getObjectKey());
              deleteR2Quietly(storage.getDanfeObjectKey());
              fiscalNoteXmlStorageRepository.delete(storage);
            });
  }

  /**
   * Remove um objeto do R2 sem falhar a transação de hard-delete inteira: um objeto órfão no
   * bucket, que requer limpeza manual, é bem menos grave que travar o cancelamento do agrupamento
   * por uma falha transitória de armazenamento.
   */
  private void deleteR2Quietly(String key) {
    if (key == null || key.isBlank()) {
      return;
    }
    try {
      r2StorageService.delete(key);
    } catch (Exception e) {
      log.error(
          "[CombinedScoreCancellation] Falha ao remover arquivo do R2 (key={}) — requer limpeza"
              + " manual no bucket",
          key,
          e);
    }
  }
}
