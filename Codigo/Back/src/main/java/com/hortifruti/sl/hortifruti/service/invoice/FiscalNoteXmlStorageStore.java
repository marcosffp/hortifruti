package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.dto.invoice.FiscalNoteXmlStorageResponse;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.model.FileStatus;
import com.hortifruti.sl.hortifruti.model.invoice.FiscalNoteXmlStorage;
import com.hortifruti.sl.hortifruti.repository.invoice.FiscalNoteXmlStorageRepository;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import com.hortifruti.sl.hortifruti.service.storage.StorageKeyGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Orquestra a persistência da NF (upload no R2 + linha no banco) e sua leitura. Desfaz o upload no
 * R2 quando perde a corrida de {@code ref} (ver {@link FiscalNoteRefLock}) contra a constraint
 * UNIQUE(ref) do banco, evitando arquivo órfão no bucket.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class FiscalNoteXmlStorageStore {

  private final FiscalNoteXmlStorageRepository repository;
  private final R2StorageService r2StorageService;
  private final FiscalNoteRefLock refLock;
  private final FiscalNoteFocusNfeClient focusNfeClient;

  @Value("${r2.environment}")
  private String environment;

  boolean existsByRef(String ref) {
    return repository.existsByRef(ref);
  }

  void persistIfAbsent(String ref, String xmlContent, byte[] danfeBytes, NfMetadata metadata) {
    refLock.withLock(ref, () -> persistIfAbsentLocked(ref, xmlContent, danfeBytes, metadata));
  }

  private void persistIfAbsentLocked(
      String ref, String xmlContent, byte[] danfeBytes, NfMetadata metadata) {
    if (repository.existsByRef(ref)) {
      return;
    }

    String xmlKey = StorageKeyGenerator.generate("notas-fiscais", environment, ref, "xml");
    String danfeKey = null;
    try {
      r2StorageService.upload(xmlContent.getBytes(), xmlKey, "application/xml");

      if (danfeBytes != null && danfeBytes.length > 0) {
        danfeKey = StorageKeyGenerator.generate("notas-fiscais", environment, ref, "pdf");
        r2StorageService.upload(danfeBytes, danfeKey, "application/pdf");
      }

      FiscalNoteXmlStorage storage =
          FiscalNoteXmlStorage.builder()
              .ref(ref)
              .nfNumber(metadata.nfNumber())
              .clientName(metadata.clientName())
              .totalValue(metadata.totalValue())
              .issuedAt(metadata.issuedAt())
              .objectKey(xmlKey)
              .danfeObjectKey(danfeKey)
              .build();

      repository.saveAndFlush(storage);
    } catch (DataIntegrityViolationException e) {
      log.warn(
          "[FiscalNoteXmlStorage] Colisão ao persistir ref={}, removendo upload duplicado: {}",
          ref,
          e.getMessage());
      deleteUploadedQuietly(xmlKey);
      deleteUploadedQuietly(danfeKey);
    }
  }

  void saveDanfeIfAbsent(String ref, byte[] danfeBytes) {
    if (danfeBytes == null || danfeBytes.length == 0) return;
    refLock.withLock(ref, () -> saveDanfeIfAbsentLocked(ref, danfeBytes));
  }

  private void saveDanfeIfAbsentLocked(String ref, byte[] danfeBytes) {
    FiscalNoteXmlStorage storage = repository.findByRef(ref).orElse(null);
    if (storage != null && storage.getDanfeObjectKey() != null) return;

    String key = null;
    try {
      key = StorageKeyGenerator.generate("notas-fiscais", environment, ref, "pdf");
      r2StorageService.upload(danfeBytes, key, "application/pdf");

      if (storage == null) {
        JsonNode rootNode = focusNfeClient.fetchStatus(ref);
        NfMetadata metadata = focusNfeClient.extractMetadata(rootNode, ref);
        storage =
            FiscalNoteXmlStorage.builder()
                .ref(ref)
                .nfNumber(metadata.nfNumber())
                .clientName(metadata.clientName())
                .totalValue(metadata.totalValue())
                .issuedAt(metadata.issuedAt())
                .danfeObjectKey(key)
                .build();
      } else {
        storage.setDanfeObjectKey(key);
      }
      repository.saveAndFlush(storage);
    } catch (DataIntegrityViolationException e) {
      log.warn(
          "[FiscalNoteXmlStorage] Colisão ao salvar DANFE para ref={}, removendo upload"
              + " duplicado: {}",
          ref,
          e.getMessage());
      deleteUploadedQuietly(key);
    } catch (Exception e) {
      log.warn(
          "[FiscalNoteXmlStorage] Não foi possível salvar DANFE como backup para ref={}: {}",
          ref,
          e.getMessage());
      deleteUploadedQuietly(key);
    }
  }

  List<FiscalNoteXmlStorageResponse> findByPeriod(LocalDate startDate, LocalDate endDate) {
    return repository.findByIssuedAtBetweenOrderByIssuedAtDesc(startDate, endDate).stream()
        .map(this::toResponse)
        .toList();
  }

  byte[] getXmlContent(String ref) {
    FiscalNoteXmlStorage storage =
        repository
            .findByRef(ref)
            .orElseThrow(
                () -> new InvoiceException("XML não encontrado no banco para ref: " + ref));
    if (storage.getObjectKey() != null) {
      return r2StorageService.download(storage.getObjectKey());
    }
    return storage.getXmlContent().getBytes();
  }

  /**
   * Retorna o DANFE (PDF) já persistido no R2 para o ref informado, ou {@code null} se ainda não
   * foi salvo — o chamador deve então buscar ao vivo na Focus NFe e salvar via {@link
   * #saveDanfeIfAbsent}.
   */
  byte[] getDanfeContent(String ref) {
    return repository
        .findByRef(ref)
        .map(FiscalNoteXmlStorage::getDanfeObjectKey)
        .filter(key -> key != null && !key.isBlank())
        .map(r2StorageService::download)
        .orElse(null);
  }

  void cancelXmlFile(String ref) {
    try {
      FiscalNoteXmlStorage storage = repository.findByRef(ref).orElse(null);
      if (storage == null || storage.getObjectKey() == null) {
        log.warn(
            "[FiscalNoteXmlStorage] Cancelamento confirmado para ref={}, mas nenhum arquivo"
                + " ativo encontrado no R2 para mover",
            ref);
        return;
      }

      String destinationKey =
          StorageKeyGenerator.withCancelledSegment(storage.getObjectKey(), "canceladas");
      r2StorageService.moveToCancelled(storage.getObjectKey(), destinationKey);

      storage.setObjectKey(destinationKey);
      storage.setStatus(FileStatus.CANCELLED);
      storage.setCancelledAt(LocalDateTime.now());
      repository.save(storage);
    } catch (Exception e) {
      log.error(
          "[FiscalNoteXmlStorage] Falha ao mover XML da NF para canceladas (ref={}): {} — requer"
              + " verificação manual no bucket",
          ref,
          e.getMessage(),
          e);
    }
  }

  String getNfNumber(String ref) {
    return repository.findByRef(ref).map(FiscalNoteXmlStorage::getNfNumber).orElse(ref);
  }

  private void deleteUploadedQuietly(String key) {
    if (key == null) return;
    try {
      r2StorageService.delete(key);
    } catch (Exception e) {
      log.error(
          "[FiscalNoteXmlStorage] Falha ao remover upload órfão key={} após colisão de ref: {}"
              + " — requer limpeza manual no bucket",
          key,
          e.getMessage(),
          e);
    }
  }

  private FiscalNoteXmlStorageResponse toResponse(FiscalNoteXmlStorage s) {
    return new FiscalNoteXmlStorageResponse(
        s.getId(),
        s.getRef(),
        s.getNfNumber(),
        s.getClientName(),
        s.getTotalValue(),
        s.getIssuedAt(),
        s.getCreatedAt());
  }
}
