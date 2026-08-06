package com.hortifruti.sl.hortifruti.service.storage;

import com.hortifruti.sl.hortifruti.exception.storage.StorageNotFoundException;
import com.hortifruti.sl.hortifruti.model.FileStatus;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScorePhotoFile;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScorePhotoFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mesmo padrão de {@link BilletFileStorageService}, para o PDF de fotos do agrupamento. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CombinedScorePhotoFileStorageService {

  private final R2StorageService r2StorageService;
  private final CombinedScorePhotoFileRepository combinedScorePhotoFileRepository;

  @Value("${r2.environment}")
  private String environment;

  /**
   * Envia o PDF de fotos para o R2 e registra a chave. Idempotente: se já existe um arquivo ATIVO
   * para este combinedScoreId, reaproveita em vez de duplicar.
   */
  @Transactional
  public CombinedScorePhotoFile savePhotoFile(Long combinedScoreId, byte[] pdfBytes) {
    var existing =
        combinedScorePhotoFileRepository.findByCombinedScoreIdAndStatus(
            combinedScoreId, FileStatus.ACTIVE);
    if (existing.isPresent()) {
      log.info(
          "[CombinedScorePhotoFileStorage] combinedScoreId={} já possui PDF de fotos ativo"
              + " (key={}), reaproveitando",
          combinedScoreId,
          existing.get().getObjectKey());
      return existing.get();
    }

    String key =
        StorageKeyGenerator.generate(
            "fotos-agrupamento", environment, String.valueOf(combinedScoreId), "pdf");
    r2StorageService.upload(pdfBytes, key, "application/pdf");

    CombinedScorePhotoFile photoFile =
        CombinedScorePhotoFile.builder().combinedScoreId(combinedScoreId).objectKey(key).build();
    return combinedScorePhotoFileRepository.save(photoFile);
  }

  /**
   * Baixa do R2 o PDF de fotos ativo associado ao combinedScoreId. Lança StorageNotFoundException
   * (404) se ainda não houver um gerado.
   */
  public byte[] getPhotoFileContent(Long combinedScoreId) {
    CombinedScorePhotoFile photoFile =
        combinedScorePhotoFileRepository
            .findByCombinedScoreIdAndStatus(combinedScoreId, FileStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new StorageNotFoundException(
                        "Nenhum PDF de fotos foi encontrado para este agrupamento."));
    return r2StorageService.download(photoFile.getObjectKey());
  }
}
