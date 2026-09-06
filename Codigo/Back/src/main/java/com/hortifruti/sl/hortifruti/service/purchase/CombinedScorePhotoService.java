package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.exception.storage.StorageNotFoundException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import com.hortifruti.sl.hortifruti.service.storage.CombinedScorePhotoFileStorageService;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera e serve o PDF com as fotos de comprovante das compras de um agrupamento (uma foto por compra
 * que tenha {@link Purchase#getImagemR2Key()} preenchido — só acontece para clientes que exigem
 * comprovante, ver {@code Client#requiresPurchaseProof}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CombinedScorePhotoService {

  private final PurchaseRepository purchaseRepository;
  private final R2StorageService r2StorageService;
  private final CombinedScorePhotoFileStorageService photoFileStorageService;
  private final PurchasePhotosPdfGenerator pdfGenerator;
  private final CombinedScoreRepository combinedScoreRepository;
  private final ClientRepository clientRepository;

  /**
   * Gera e armazena o PDF de fotos deste agrupamento, se houver ao menos uma compra com foto e
   * ainda não existir um PDF armazenado (idempotente, ver {@link
   * CombinedScorePhotoFileStorageService#savePhotoFile}). Best-effort: uma falha aqui não deve
   * impedir a criação do agrupamento em si — o PDF pode ser gerado sob demanda depois, ver {@link
   * #getStoredPhotosPdf}.
   */
  @Transactional
  public void generatePhotosPdfIfNeeded(Long combinedScoreId) {
    List<Purchase> purchasesWithPhotos =
        purchaseRepository.findByCombinedScoreIdAndImagemR2KeyIsNotNull(combinedScoreId);
    if (purchasesWithPhotos.isEmpty()) {
      return;
    }

    try {
      List<byte[]> images =
          purchasesWithPhotos.stream()
              .map(purchase -> r2StorageService.download(purchase.getImagemR2Key()))
              .toList();
      byte[] pdfBytes = pdfGenerator.generate(images);
      photoFileStorageService.savePhotoFile(combinedScoreId, pdfBytes);
    } catch (IOException e) {
      log.error(
          "[CombinedScorePhoto] Falha ao gerar PDF de fotos para combinedScoreId={}: {} — será"
              + " tentado novamente na próxima tentativa de download",
          combinedScoreId,
          e.getMessage(),
          e);
    }
  }

  /**
   * Baixa o PDF de fotos já armazenado. Se ainda não existir (ex: agrupamento criado antes desta
   * funcionalidade existir, ou geração anterior falhou), tenta gerar na hora antes de desistir.
   */
  public ResponseEntity<byte[]> getStoredPhotosPdf(Long combinedScoreId) {
    byte[] pdfBytes;
    try {
      pdfBytes = photoFileStorageService.getPhotoFileContent(combinedScoreId);
    } catch (StorageNotFoundException e) {
      generatePhotosPdfIfNeeded(combinedScoreId);
      pdfBytes = photoFileStorageService.getPhotoFileContent(combinedScoreId);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", buildFileName(combinedScoreId));
    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }

  private String buildFileName(Long combinedScoreId) {
    String clientName =
        combinedScoreRepository
            .findById(combinedScoreId)
            .map(CombinedScore::getClientId)
            .flatMap(clientRepository::findById)
            .map(Client::getClientName)
            .orElse(null);

    if (clientName == null || clientName.isBlank()) {
      return "NOTINHAS_" + combinedScoreId + ".pdf";
    }
    return "NOTINHAS_" + toFileNameSafe(firstName(clientName)) + ".pdf";
  }

  private String firstName(String value) {
    return value.trim().split("\\s+")[0];
  }

  private String toFileNameSafe(String value) {
    String withoutAccents =
        Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    String sanitized = withoutAccents.toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    return sanitized.replaceAll("^_+|_+$", "");
  }
}
