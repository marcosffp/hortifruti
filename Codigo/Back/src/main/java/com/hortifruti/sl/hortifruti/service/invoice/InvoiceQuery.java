package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseSimplif;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
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

  private final int COMPLETE = 1;

  @Transactional
  protected InvoiceResponseGet consultInvoice(String ref) {
    try {
      String response = fetchInvoiceData(ref);
      JsonNode rootNode = parseJson(response);
      InvoiceResponseSimplif invoiceSimplif = extractInvoiceData(rootNode);
      Client client = findClient(invoiceSimplif.cnpjDestinatario());
      return buildInvoiceResponse(invoiceSimplif, client, ref);
    } catch (Exception e) {
      throw new InvoiceException("Erro ao consultar a nota fiscal com referência: " + ref, e);
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
    String dataEmissaoStr = requisicaoNode.path("data_emissao").asText();
    LocalDateTime dataEmissao = OffsetDateTime.parse(dataEmissaoStr).toLocalDateTime();

    return new InvoiceResponseSimplif(
        requisicaoNode.path("cnpj_destinatario").asText(),
        new BigDecimal(requisicaoNode.path("valor_total").asText()),
        requisicaoNode.path("numero").asText(),
        rootNode.path("status").asText(),
        dataEmissao,
        requisicaoNode.path("ref").asText());
  }

  private Client findClient(String cnpjDestinatario) {
    return clientRepository
        .findByDocument(cnpjDestinatario)
        .orElseThrow(
            () -> new InvoiceException("Cliente não encontrado para o CNPJ: " + cnpjDestinatario));
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

  /**
   * Lista todas as referências (refs) de notas fiscais para um CPF/CNPJ
   * 
   * @param cpfCnpj CPF ou CNPJ do cliente (apenas números)
   * @return Lista de referências das notas fiscais autorizadas
   */
  @Transactional
  protected List<String> listInvoiceRefsByDocument(String cpfCnpj) {
    List<String> refs = new ArrayList<>();
    try {
      log.info("Buscando notas fiscais na API Focus NFe para CPF/CNPJ: {}", cpfCnpj);
      String response = focusNfeApiClient.listInvoicesByDocument(cpfCnpj);
      
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode rootNode = objectMapper.readTree(response);
      
      // A resposta da API Focus NFe retorna um array de notas
      if (rootNode.isArray()) {
        for (JsonNode invoiceNode : rootNode) {
          String status = invoiceNode.path("status").asText();
          String ref = invoiceNode.path("ref").asText();
          
          // Apenas notas autorizadas
          if ("autorizado".equalsIgnoreCase(status) && ref != null && !ref.isEmpty()) {
            refs.add(ref);
            log.debug("Nota fiscal autorizada encontrada - Ref: {}, Status: {}", ref, status);
          }
        }
      }
      
      log.info("Total de notas fiscais autorizadas encontradas para {}: {}", cpfCnpj, refs.size());
      return refs;
    } catch (Exception e) {
      log.error("Erro ao listar notas fiscais por CPF/CNPJ {}: {}", cpfCnpj, e.getMessage(), e);
      // Retorna lista vazia em vez de lançar exceção para não quebrar o fluxo
      return refs;
    }
  }
}
