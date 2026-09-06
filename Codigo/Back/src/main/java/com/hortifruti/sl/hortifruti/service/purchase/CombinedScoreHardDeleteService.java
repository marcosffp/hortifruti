package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.exception.purchase.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.billet.BilletFile;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.model.purchase.Status;
import com.hortifruti.sl.hortifruti.repository.billet.BilletFileRepository;
import com.hortifruti.sl.hortifruti.repository.invoice.FiscalNoteXmlStorageRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apaga localmente o que resta de um agrupamento (CombinedScore) já sem NF-e/boleto ativos.
 * Extraído de {@link CombinedScoreCancellationService} para uma classe própria: {@code
 * hardDeleteLocally} precisa de {@code @Transactional} real (a query de desvínculo das compras é
 * um bulk update, que exige transação ativa), e chamar um método {@code @Transactional} a partir
 * de outro método do mesmo bean (self-invocation) não passa pelo proxy do Spring — a anotação
 * seria silenciosamente ignorada.
 */
@Slf4j
@Service
@AllArgsConstructor
public class CombinedScoreHardDeleteService {

  private final CombinedScoreService combinedScoreService;
  private final CombinedScoreRepository combinedScoreRepository;
  private final BilletFileRepository billetFileRepository;
  private final FiscalNoteXmlStorageRepository fiscalNoteXmlStorageRepository;
  private final PurchaseRepository purchaseRepository;
  private final R2StorageService r2StorageService;

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
    // pra um novo agrupamento (ver PurchaseRepository#clearCombinedScoreId). As compras em si NUNCA
    // são apagadas aqui — só o vínculo com o agrupamento.
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
