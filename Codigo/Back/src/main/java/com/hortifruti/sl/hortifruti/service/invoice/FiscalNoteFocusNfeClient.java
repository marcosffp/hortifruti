package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.purchase.ClientService;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Encapsula toda a comunicação HTTP direta com a Focus NFe — consulta de status e download de
 * XML/DANFE — e a extração dos metadados fiscais (número, cliente, valor, data de emissão) da
 * resposta dela.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class FiscalNoteFocusNfeClient {

  private static final int COMPLETE = 1;

  private final FocusNfeApiClient focusNfeApiClient;
  private final ObjectMapper objectMapper;
  private final CombinedScoreService combinedScoreService;
  private final ClientService clientService;

  JsonNode fetchStatus(String ref) throws IOException {
    String response = focusNfeApiClient.sendGetRequest(ref, COMPLETE);
    return objectMapper.readTree(response);
  }

  byte[] downloadFileBytes(String filePath, MediaType mediaType) {
    try {
      return focusNfeApiClient.downloadFile(filePath, mediaType);
    } catch (Exception e) {
      log.error(
          "[FiscalNoteXmlStorage] Erro ao baixar arquivo ({}): {}", mediaType, e.getMessage());
      return null;
    }
  }

  /**
   * Best-effort: tenta ler {@code caminho_danfe} do JSON já obtido da Focus NFe e baixar o PDF.
   * Retorna {@code null} em qualquer falha (path ausente, download vazio, etc.) — a ausência do
   * DANFE aqui não deve impedir o salvamento do XML; a rede de segurança em {@code
   * FiscalNoteXmlStorageStore#saveDanfeIfAbsent} cobre isso depois, na primeira tentativa de
   * download.
   */
  byte[] downloadDanfeBytesBestEffort(JsonNode rootNode, String ref) {
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

  NfMetadata extractMetadata(JsonNode rootNode, String ref) {
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
}
