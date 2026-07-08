package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.dto.invoice.FiscalNoteXmlStorageResponse;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import com.hortifruti.sl.hortifruti.model.invoice.FiscalNoteXmlStorage;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.invoice.FiscalNoteXmlStorageRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalNoteXmlStorageService {

  private final FiscalNoteXmlStorageRepository repository;
  private final FocusNfeApiClient focusNfeApiClient;
  private final WebClient webClient;
  private final CombinedScoreRepository combinedScoreRepository;
  private final ClientRepository clientRepository;
  private final ObjectMapper objectMapper;

  @Value("${focus.nfe.api.url}")
  private String focusNfeApiUrl;

  private static final int MAX_POLL_ATTEMPTS = 36;
  private static final long POLL_INTERVAL_MS = 10_000;
  private static final int COMPLETE = 1;

  /** Triggered async right after invoice issuance. Polls until authorized, then saves XML. */
  @Async
  public void triggerSaveAfterIssuance(String ref) {
    log.info("[FiscalNoteXmlStorage] Iniciando salvamento assíncrono para ref={}", ref);
    try {
      for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
        Thread.sleep(POLL_INTERVAL_MS);

        String response = focusNfeApiClient.sendGetRequest(ref, COMPLETE);
        JsonNode rootNode = objectMapper.readTree(response);
        String status = rootNode.path("status").asText();

        log.info(
            "[FiscalNoteXmlStorage] Polling ref={} attempt={} status={}", ref, attempt, status);

        if (status.contains("autorizado")) {
          String xmlPath = rootNode.path("caminho_xml_nota_fiscal").asText();
          if (xmlPath == null || xmlPath.isBlank()) {
            log.warn("[FiscalNoteXmlStorage] XML path vazio para ref={}, aguardando...", ref);
            continue;
          }

          byte[] xmlBytes = downloadXmlBytes(xmlPath);
          if (xmlBytes == null || xmlBytes.length == 0) {
            log.warn("[FiscalNoteXmlStorage] XML bytes vazios para ref={}", ref);
            continue;
          }

          NfMetadata metadata = extractMetadata(rootNode, ref);
          persistIfAbsent(ref, new String(xmlBytes), metadata);
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

  /** Called from downloadXml as a safety net — saves if not yet persisted. */
  @Transactional
  public void saveIfAbsent(String ref, byte[] xmlBytes) {
    if (repository.existsByRef(ref)) return;
    try {
      String response = focusNfeApiClient.sendGetRequest(ref, COMPLETE);
      JsonNode rootNode = objectMapper.readTree(response);
      NfMetadata metadata = extractMetadata(rootNode, ref);
      persistIfAbsent(ref, new String(xmlBytes), metadata);
    } catch (Exception e) {
      log.warn(
          "[FiscalNoteXmlStorage] Não foi possível salvar XML como backup para ref={}: {}",
          ref,
          e.getMessage());
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
    return storage.getXmlContent().getBytes();
  }

  @Transactional
  public String getNfNumber(String ref) {
    return repository.findByRef(ref).map(FiscalNoteXmlStorage::getNfNumber).orElse(ref);
  }

  private void persistIfAbsent(String ref, String xmlContent, NfMetadata metadata) {
    if (repository.existsByRef(ref)) {
      log.info("[FiscalNoteXmlStorage] ref={} já existe no banco, ignorando.", ref);
      return;
    }

    FiscalNoteXmlStorage storage =
        FiscalNoteXmlStorage.builder()
            .ref(ref)
            .nfNumber(metadata.nfNumber())
            .clientName(metadata.clientName())
            .totalValue(metadata.totalValue())
            .issuedAt(metadata.issuedAt())
            .xmlContent(xmlContent)
            .build();

    repository.save(storage);
    log.info(
        "[FiscalNoteXmlStorage] XML salvo com sucesso: ref={} nfNumber={}",
        ref,
        metadata.nfNumber());
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
      return combinedScoreRepository
          .findByInvoiceRef(ref)
          .map(CombinedScore::getClientId)
          .flatMap(clientRepository::findById)
          .map(Client::getClientName)
          .orElse("Cliente não identificado");
    } catch (Exception e) {
      return "Cliente não identificado";
    }
  }

  private byte[] downloadXmlBytes(String xmlPath) {
    try {
      String fullUrl = focusNfeApiUrl + xmlPath;
      return webClient
          .get()
          .uri(fullUrl)
          .accept(MediaType.APPLICATION_XML)
          .retrieve()
          .bodyToMono(byte[].class)
          .timeout(java.time.Duration.ofSeconds(60))
          .block();
    } catch (Exception e) {
      log.error("[FiscalNoteXmlStorage] Erro ao baixar XML: {}", e.getMessage());
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
