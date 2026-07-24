package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.dto.invoice.FiscalNoteXmlStorageResponse;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.model.FileStatus;
import com.hortifruti.sl.hortifruti.model.invoice.FiscalNoteXmlStorage;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.invoice.FiscalNoteXmlStorageRepository;
import com.hortifruti.sl.hortifruti.service.purchase.ClientService;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import com.hortifruti.sl.hortifruti.service.storage.StorageKeyGenerator;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalNoteXmlStorageService {

  private final FiscalNoteXmlStorageRepository repository;
  private final FocusNfeApiClient focusNfeApiClient;
  private final WebClient webClient;
  private final CombinedScoreService combinedScoreService;
  private final ClientService clientService;
  private final ObjectMapper objectMapper;
  private final R2StorageService r2StorageService;

  @Value("${focus.nfe.api.url}")
  private String focusNfeApiUrl;

  @Value("${r2.environment}")
  private String environment;

  private static final int MAX_POLL_ATTEMPTS = 36;
  private static final long POLL_INTERVAL_MS = 10_000;
  private static final int COMPLETE = 1;

  /**
   * Serializa, por ref, o trecho check-then-upload-then-save de {@link #persistIfAbsent} e {@link
   * #saveDanfeIfAbsent}. Sem isso, o job assíncrono {@link #triggerSaveAfterIssuance} e a rede de
   * segurança do download (disparada se o usuário abrir o XML/DANFE antes do job terminar) podem
   * checar "existsByRef" ao mesmo tempo, os dois verem "não existe" e os dois fazerem upload para
   * o R2 — gerando um arquivo duplicado órfão no bucket quando o segundo INSERT falha por
   * violação da constraint UNIQUE(ref). A aplicação roda como instância única (ver
   * docker-compose.yml), então este lock em memória fecha a corrida na prática; a captura de
   * {@link DataIntegrityViolationException} abaixo é uma segunda camada de defesa.
   */
  private final ConcurrentHashMap<String, ReentrantLock> refLocks = new ConcurrentHashMap<>();

  private void withRefLock(String ref, Runnable action) {
    ReentrantLock lock = refLocks.computeIfAbsent(ref, k -> new ReentrantLock());
    lock.lock();
    try {
      action.run();
    } finally {
      lock.unlock();
    }
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

  /** Triggered async right after invoice issuance. Polls until authorized, then saves XML. */
  @Async
  public void triggerSaveAfterIssuance(String ref) {
    try {
      for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
        Thread.sleep(POLL_INTERVAL_MS);

        String response = focusNfeApiClient.sendGetRequest(ref, COMPLETE);
        JsonNode rootNode = objectMapper.readTree(response);
        String status = rootNode.path("status").asText();

        if (status.contains("autorizado")) {
          String xmlPath = rootNode.path("caminho_xml_nota_fiscal").asText();
          if (xmlPath == null || xmlPath.isBlank()) {
            log.warn("[FiscalNoteXmlStorage] XML path vazio para ref={}, aguardando...", ref);
            continue;
          }

          byte[] xmlBytes = downloadFileBytes(xmlPath, MediaType.APPLICATION_XML);
          if (xmlBytes == null || xmlBytes.length == 0) {
            log.warn("[FiscalNoteXmlStorage] XML bytes vazios para ref={}", ref);
            continue;
          }

          byte[] danfeBytes = downloadDanfeBytesBestEffort(rootNode, ref);

          NfMetadata metadata = extractMetadata(rootNode, ref);
          persistIfAbsent(ref, new String(xmlBytes), danfeBytes, metadata);
          return;
        }

        if (status.contains("cancelado")
            || status.contains("denegado")
            || status.contains("erro")) {
          log.warn(
              "[FiscalNoteXmlStorage] NF ref={} com status={}, salvamento cancelado", ref, status);
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
   * Called from downloadXml as a safety net — saves if not yet persisted.
   *
   * <p>Roda em transação própria (REQUIRES_NEW): pode ser chamado ao mesmo tempo que o job
   * assíncrono {@link #triggerSaveAfterIssuance} para a mesma ref; {@link #persistIfAbsent}
   * serializa esse trecho via {@link #refLocks} e desfaz o upload no R2 se ainda assim perder a
   * corrida, então isso não gera arquivo duplicado. Uma falha aqui é só best-effort — não pode
   * envenenar a transação de quem chamou (ex: downloadXml/downloadDanfe), senão o commit do
   * chamador falha com UnexpectedRollbackException mesmo com a exceção sendo capturada abaixo.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void saveIfAbsent(String ref, byte[] xmlBytes) {
    if (repository.existsByRef(ref)) return;
    try {
      String response = focusNfeApiClient.sendGetRequest(ref, COMPLETE);
      JsonNode rootNode = objectMapper.readTree(response);
      NfMetadata metadata = extractMetadata(rootNode, ref);
      byte[] danfeBytes = downloadDanfeBytesBestEffort(rootNode, ref);
      persistIfAbsent(ref, new String(xmlBytes), danfeBytes, metadata);
    } catch (Exception e) {
      log.warn(
          "[FiscalNoteXmlStorage] Não foi possível salvar XML como backup para ref={}: {}",
          ref,
          e.getMessage());
    }
  }

  /**
   * Called from DanfeXmlService.downloadDanfe as a safety net — saves the DANFE if not yet
   * persisted. Unlike {@link #saveIfAbsent}, this only touches the {@code danfeObjectKey} field:
   * the row may already exist (XML saved earlier) or not (DANFE requested before XML was ever
   * persisted).
   *
   * <p>Roda em transação própria (REQUIRES_NEW) pelo mesmo motivo de {@link #saveIfAbsent}.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void saveDanfeIfAbsent(String ref, byte[] danfeBytes) {
    if (danfeBytes == null || danfeBytes.length == 0) return;
    withRefLock(ref, () -> saveDanfeIfAbsentLocked(ref, danfeBytes));
  }

  private void saveDanfeIfAbsentLocked(String ref, byte[] danfeBytes) {
    FiscalNoteXmlStorage storage = repository.findByRef(ref).orElse(null);
    if (storage != null && storage.getDanfeObjectKey() != null) return;

    String key = null;
    try {
      key = StorageKeyGenerator.generate("notas-fiscais", environment, ref, "pdf");
      r2StorageService.upload(danfeBytes, key, "application/pdf");

      if (storage == null) {
        String response = focusNfeApiClient.sendGetRequest(ref, COMPLETE);
        JsonNode rootNode = objectMapper.readTree(response);
        NfMetadata metadata = extractMetadata(rootNode, ref);
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

  @Transactional
  public List<FiscalNoteXmlStorageResponse> findByPeriod(LocalDate startDate, LocalDate endDate) {
    return repository.findByIssuedAtBetweenOrderByIssuedAtDesc(startDate, endDate).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public byte[] getXmlContent(String ref) {
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
  @Transactional
  public byte[] getDanfeContent(String ref) {
    return repository
        .findByRef(ref)
        .map(FiscalNoteXmlStorage::getDanfeObjectKey)
        .filter(key -> key != null && !key.isBlank())
        .map(r2StorageService::download)
        .orElse(null);
  }

  /** Agenda o cancelamento (mover para canceladas/) para depois do commit da transação corrente. */
  public void cancelXmlFileAfterCommit(String ref) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              cancelXmlFile(ref);
            }
          });
    } else {
      cancelXmlFile(ref);
    }
  }

  @Transactional
  public void cancelXmlFile(String ref) {
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

  @Transactional
  public String getNfNumber(String ref) {
    return repository.findByRef(ref).map(FiscalNoteXmlStorage::getNfNumber).orElse(ref);
  }

  private void persistIfAbsent(
      String ref, String xmlContent, byte[] danfeBytes, NfMetadata metadata) {
    withRefLock(ref, () -> persistIfAbsentLocked(ref, xmlContent, danfeBytes, metadata));
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

  /**
   * Best-effort: tenta ler {@code caminho_danfe} do JSON já obtido da Focus NFe e baixar o PDF.
   * Retorna {@code null} em qualquer falha (path ausente, download vazio, etc.) — a ausência do
   * DANFE aqui não deve impedir o salvamento do XML; a rede de segurança em {@link
   * #saveDanfeIfAbsent} cobre isso depois, na primeira tentativa de download.
   */
  private byte[] downloadDanfeBytesBestEffort(JsonNode rootNode, String ref) {
    try {
      String danfePath = rootNode.path("caminho_danfe").asText();
      if (danfePath == null || danfePath.isBlank()) {
        return null;
      }
      return downloadFileBytes(danfePath, MediaType.APPLICATION_PDF);
    } catch (Exception e) {
      log.warn(
          "[FiscalNoteXmlStorage] Não foi possível baixar DANFE junto ao XML para ref={}: {}",
          ref,
          e.getMessage());
      return null;
    }
  }

  private NfMetadata extractMetadata(JsonNode rootNode, String ref) {
    JsonNode requisicaoNode = rootNode.path("requisicao_nota_fiscal");

    String nfNumero = rootNode.path("numero").asText();
    if (nfNumero == null || nfNumero.isBlank()) {
      nfNumero = requisicaoNode.path("numero").asText("0");
    }
    String nfNumber = "NF-" + nfNumero;

    BigDecimal totalValue;
    try {
      totalValue = new BigDecimal(requisicaoNode.path("valor_total").asText("0"));
    } catch (Exception e) {
      totalValue = BigDecimal.ZERO;
    }

    LocalDate issuedAt;
    try {
      String dataEmissaoStr = requisicaoNode.path("data_emissao").asText();
      issuedAt = OffsetDateTime.parse(dataEmissaoStr).toLocalDate();
    } catch (Exception e) {
      issuedAt = LocalDate.now();
    }

    String clientName = resolveClientName(ref);

    return new NfMetadata(nfNumber, clientName, totalValue, issuedAt);
  }

  private String resolveClientName(String ref) {
    try {
      return combinedScoreService
          .findByInvoiceRef(ref)
          .map(CombinedScore::getClientId)
          .flatMap(clientService::findClientName)
          .orElse("Cliente não identificado");
    } catch (Exception e) {
      return "Cliente não identificado";
    }
  }

  private byte[] downloadFileBytes(String filePath, MediaType mediaType) {
    try {
      String fullUrl = focusNfeApiUrl + filePath;
      return webClient
          .get()
          .uri(fullUrl)
          .accept(mediaType)
          .retrieve()
          .bodyToMono(byte[].class)
          .timeout(java.time.Duration.ofSeconds(60))
          .block();
    } catch (Exception e) {
      log.error(
          "[FiscalNoteXmlStorage] Erro ao baixar arquivo ({}): {}", mediaType, e.getMessage());
      return null;
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

  private record NfMetadata(
      String nfNumber, String clientName, BigDecimal totalValue, LocalDate issuedAt) {}
}
