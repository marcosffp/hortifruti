package com.hortifruti.sl.hortifruti.service.storage;

import com.hortifruti.sl.hortifruti.exception.StorageException;
import com.hortifruti.sl.hortifruti.model.billet.BilletFile;
import com.hortifruti.sl.hortifruti.repository.billet.BilletFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class BilletFileStorageService {

  private final R2StorageService r2StorageService;
  private final BilletFileRepository billetFileRepository;

  @Value("${r2.environment}")
  private String environment;

  /**
   * Envia o PDF do boleto para o R2 e registra a chave. Idempotente: se já existe um arquivo ATIVO
   * para este combinedScoreId (reprocessamento/retry), reaproveita em vez de duplicar. Lança
   * StorageException se o upload falhar — a geração do boleto não deve ser considerada concluída.
   */
  @Transactional
  public BilletFile saveBilletFile(Long combinedScoreId, byte[] pdfBytes) {
    var existing =
        billetFileRepository.findByCombinedScoreIdAndStatus(
            combinedScoreId, BilletFile.Status.ACTIVE);
    if (existing.isPresent()) {
      log.info(
          "[BilletFileStorage] combinedScoreId={} já possui arquivo ativo (key={}),"
              + " reaproveitando — reprocessamento/retry detectado",
          combinedScoreId,
          existing.get().getObjectKey());
      return existing.get();
    }

    String key =
        StorageKeyGenerator.generate(
            "boletos", environment, String.valueOf(combinedScoreId), "pdf");
    r2StorageService.upload(pdfBytes, key, "application/pdf");

    BilletFile billetFile =
        BilletFile.builder().combinedScoreId(combinedScoreId).objectKey(key).build();
    return billetFileRepository.save(billetFile);
  }

  /**
   * Baixa do R2 o PDF do boleto ativo associado ao combinedScoreId. Lança StorageException se não
   * houver arquivo ativo (ex: boleto cancelado, ou gerado antes desta funcionalidade existir).
   */
  public byte[] getBilletFileContent(Long combinedScoreId) {
    BilletFile billetFile =
        billetFileRepository
            .findByCombinedScoreIdAndStatus(combinedScoreId, BilletFile.Status.ACTIVE)
            .orElseThrow(
                () ->
                    new StorageException(
                        "Nenhum boleto armazenado encontrado para o agrupamento "
                            + combinedScoreId));
    return r2StorageService.download(billetFile.getObjectKey());
  }

  /**
   * Agenda a movimentação do arquivo do boleto para a pasta de cancelados, para ser executada
   * somente após o commit da transação corrente (garante que o cancelamento já está confirmado no
   * banco antes de tocar no R2 — ver plano de implementação).
   */
  public void cancelBilletFileAfterCommit(Long combinedScoreId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              cancelBilletFile(combinedScoreId);
            }
          });
    } else {
      cancelBilletFile(combinedScoreId);
    }
  }

  @Transactional
  public void cancelBilletFile(Long combinedScoreId) {
    try {
      var activeFile =
          billetFileRepository.findByCombinedScoreIdAndStatus(
              combinedScoreId, BilletFile.Status.ACTIVE);
      if (activeFile.isEmpty()) {
        log.warn(
            "[BilletFileStorage] Cancelamento confirmado para combinedScoreId={}, mas nenhum"
                + " arquivo ativo encontrado no R2 para mover",
            combinedScoreId);
        return;
      }

      BilletFile billetFile = activeFile.get();
      String destinationKey =
          StorageKeyGenerator.withCancelledSegment(billetFile.getObjectKey(), "cancelados");
      r2StorageService.moveToCancelled(billetFile.getObjectKey(), destinationKey);

      billetFile.setObjectKey(destinationKey);
      billetFile.setStatus(BilletFile.Status.CANCELLED);
      billetFile.setCancelledAt(java.time.LocalDateTime.now());
      billetFileRepository.save(billetFile);
    } catch (Exception e) {
      log.error(
          "[BilletFileStorage] Falha ao mover arquivo do boleto para cancelados"
              + " (combinedScoreId={}): {} — requer verificação manual no bucket",
          combinedScoreId,
          e.getMessage(),
          e);
    }
  }
}
