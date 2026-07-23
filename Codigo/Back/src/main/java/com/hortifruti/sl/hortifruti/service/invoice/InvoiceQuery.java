package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseSimplif;
import com.hortifruti.sl.hortifruti.dto.invoice.tax.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.tax.ItemTaxDetails;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceQuery {
  private final FocusNfeApiClient focusNfeApiClient;
  private final ClientRepository clientRepository;
  private final CombinedScoreRepository combinedScoreRepository;

  private final int COMPLETE = 1;

  @Transactional
  public InvoiceResponseGet consultInvoice(String ref) {
    try {
      String response = fetchInvoiceData(ref);
      JsonNode rootNode = parseJson(response);

      validateInvoiceStatus(rootNode);

      InvoiceResponseSimplif invoiceSimplif = extractInvoiceData(rootNode);
      Client client = findClientForInvoice(ref);
      return buildInvoiceResponse(invoiceSimplif, client, ref);
    } catch (InvoiceException e) {
      throw e;
    } catch (Exception e) {
      throw new InvoiceException("Erro ao consultar a nota fiscal com referência: " + ref, e);
    }
  }

  private void validateInvoiceStatus(JsonNode rootNode) {
    String status = rootNode.path("status").asText();

    if (status.contains("processando") || status.contains("pendente")) {
      throw new InvoiceException(
          "A nota fiscal ainda está sendo processada. Status: "
              + status
              + ". Aguarde alguns instantes e tente novamente.");
    }

    JsonNode requisicaoNode = rootNode.path("requisicao_nota_fiscal");
    if (requisicaoNode.isMissingNode() || requisicaoNode.isNull() || requisicaoNode.isEmpty()) {
      throw new InvoiceException(
          "Dados da nota fiscal ainda não estão disponíveis. Status: " + status);
    }
  }

  private String fetchInvoiceData(String ref) {
    return focusNfeApiClient.sendGetRequest(ref, COMPLETE);
  }

  private JsonNode parseJson(String response) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readTree(response);
  }

  private InvoiceResponseSimplif extractInvoiceData(JsonNode rootNode) {
    JsonNode requisicaoNode = rootNode.path("requisicao_nota_fiscal");

    validateRequiredFields(requisicaoNode);

    String dataEmissaoStr = requisicaoNode.path("data_emissao").asText();
    LocalDateTime dataEmissao;

    try {
      if (dataEmissaoStr == null || dataEmissaoStr.trim().isEmpty()) {
        log.warn("Data de emissão está vazia, usando data atual como fallback");
        dataEmissao = LocalDateTime.now();
      } else {
        dataEmissao = OffsetDateTime.parse(dataEmissaoStr).toLocalDateTime();
      }
    } catch (Exception e) {
      log.warn("Erro ao converter data: {} - usando data atual como fallback", dataEmissaoStr, e);
      dataEmissao = LocalDateTime.now();
    }

    return new InvoiceResponseSimplif(
        requisicaoNode.path("cnpj_destinatario").asText(),
        new BigDecimal(requisicaoNode.path("valor_total").asText()),
        requisicaoNode.path("numero").asText(),
        rootNode.path("status").asText(),
        dataEmissao,
        rootNode.path("ref").asText());
  }

  private void validateRequiredFields(JsonNode requisicaoNode) {
    String dataEmissao = requisicaoNode.path("data_emissao").asText();
    String cnpjDestinatario = requisicaoNode.path("cnpj_destinatario").asText();
    String valorTotal = requisicaoNode.path("valor_total").asText();

    if (dataEmissao.isEmpty()) {
      throw new InvoiceException(
          "Data de emissão não disponível. A nota fiscal pode estar em processamento.");
    }
    if (cnpjDestinatario.isEmpty()) {
      throw new InvoiceException("CNPJ do destinatário não disponível na nota fiscal.");
    }
    if (valorTotal.isEmpty()) {
      throw new InvoiceException("Valor total não disponível na nota fiscal.");
    }
  }

  private Client findClientForInvoice(String ref) {
    return findClientByInvoiceRef(ref);
  }

  private Client findClientByInvoiceRef(String ref) {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findByInvoiceRef(ref)
            .orElseThrow(
                () ->
                    new InvoiceException(
                        "Agrupamento não encontrado para a nota fiscal com referência: " + ref));

    return clientRepository
        .findById(combinedScore.getClientId())
        .orElseThrow(
            () ->
                new InvoiceException(
                    "Cliente não encontrado para o agrupamento: " + combinedScore.getId()));
  }

  private InvoiceResponseGet buildInvoiceResponse(
      InvoiceResponseSimplif invoiceSimplif, Client client, String ref) {
    return new InvoiceResponseGet(
        client.getClientName(),
        invoiceSimplif.valorTotal(),
        invoiceSimplif.status(),
        invoiceSimplif.dataEmissao().toString(),
        invoiceSimplif.numero(),
        ref);
  }

  private static final int MAX_TAX_DETAILS_ATTEMPTS = 3;
  private static final long INITIAL_TAX_DETAILS_RETRY_DELAY_MS = 3000;

  @Transactional
  public InvoiceTaxDetails extractInvoiceTaxDetails(String ref) {
    long retryDelay = INITIAL_TAX_DETAILS_RETRY_DELAY_MS;
    Exception lastError = null;

    for (int attempt = 1; attempt <= MAX_TAX_DETAILS_ATTEMPTS; attempt++) {
      try {
        String response = fetchInvoiceData(ref);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response);

        return extractInvoiceData(rootNode, ref);
      } catch (Exception e) {
        lastError = e;
        log.warn(
            "Tentativa {}/{} falhou ao consultar dados fiscais da nota ref={}: {}",
            attempt,
            MAX_TAX_DETAILS_ATTEMPTS,
            ref,
            e.getMessage());
        if (attempt < MAX_TAX_DETAILS_ATTEMPTS) {
          sleepBeforeRetry(retryDelay);
          retryDelay += 2000;
        }
      }
    }

    throw new InvoiceException(
        "Erro ao consultar a nota fiscal com referência: "
            + ref
            + " após "
            + MAX_TAX_DETAILS_ATTEMPTS
            + " tentativas",
        lastError);
  }

  private void sleepBeforeRetry(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new InvoiceException("Interrompido ao aguardar nova tentativa de consulta", ie);
    }
  }

  private InvoiceTaxDetails extractInvoiceData(JsonNode rootNode, String ref) {
    JsonNode requisicaoNode = rootNode.path("requisicao_nota_fiscal");

    String dataEmissaoStr = requisicaoNode.path("data_emissao").asText();
    LocalDateTime dataEmissao;

    try {
      if (dataEmissaoStr == null || dataEmissaoStr.trim().isEmpty()) {
        log.warn("Data de emissão está vazia para ref: {}, usando data atual como fallback", ref);
        dataEmissao = LocalDateTime.now();
      } else {
        dataEmissao = OffsetDateTime.parse(dataEmissaoStr).toLocalDateTime();
      }
    } catch (Exception e) {
      log.warn(
          "Erro ao converter data para ref: {} - data: {} - usando data atual como fallback",
          ref,
          dataEmissaoStr,
          e);
      dataEmissao = LocalDateTime.now();
    }

    List<ItemTaxDetails> items = new ArrayList<>();
    JsonNode itensNode = requisicaoNode.path("itens");
    if (itensNode.isArray()) {
      for (JsonNode itemNode : itensNode) {
        ItemTaxDetails item =
            new ItemTaxDetails(
                itemNode.path("cfop").asText(),
                new BigDecimal(itemNode.path("valor_bruto").asText("0")),
                itemNode.path("icms_situacao_tributaria").asText());
        items.add(item);
      }
    }

    return new InvoiceTaxDetails(
        rootNode.path("status").asText(),
        rootNode.path("numero").asText(),
        dataEmissao,
        new BigDecimal(requisicaoNode.path("valor_produtos").asText("0")),
        new BigDecimal(requisicaoNode.path("valor_total").asText("0")),
        new BigDecimal(requisicaoNode.path("icms_base_calculo").asText("0")),
        new BigDecimal(requisicaoNode.path("icms_valor_total").asText("0")),
        items,
        ref);
  }
}
