package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseSimplif;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemTaxDetails;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
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



@Transactional
public void printRawInvoiceJson(String ref) {
  try {
    String response = fetchInvoiceData(ref);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(response);

    InvoiceTaxDetails dto = extractInvoiceData(rootNode, ref);
    System.out.println();
    System.out.println();
    System.out.println(dto);
    System.out.println();
    System.out.println();
    System.out.println();

  } catch (Exception e) {
    throw new InvoiceException("Erro ao consultar a nota fiscal com referência: " + ref, e);
  }
}


@Transactional
public InvoiceTaxDetails extractInvoiceTaxDetails(String ref) {
  try {
    String response = fetchInvoiceData(ref);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(response);

    InvoiceTaxDetails dto = extractInvoiceData(rootNode, ref);
    System.out.println();
    System.out.println();
    System.out.println(dto);
    System.out.println();
    System.out.println();
    System.out.println();
    return dto;

  } catch (Exception e) {
    throw new InvoiceException("Erro ao consultar a nota fiscal com referência: " + ref, e);
  }
}

private InvoiceTaxDetails extractInvoiceData(JsonNode rootNode, String ref) {
  JsonNode requisicaoNode = rootNode.path("requisicao_nota_fiscal");

  // Converter data
  String dataEmissaoStr = requisicaoNode.path("data_emissao").asText();
  var dataEmissao = OffsetDateTime.parse(dataEmissaoStr).toLocalDateTime();

  // Montar lista de itens
  List<ItemTaxDetails> items = new ArrayList<>();
  JsonNode itensNode = requisicaoNode.path("itens");
  if (itensNode.isArray()) {
    for (JsonNode itemNode : itensNode) {
      ItemTaxDetails item = new ItemTaxDetails(
          itemNode.path("cfop").asText(),
          new BigDecimal(itemNode.path("valor_bruto").asText("0")),
          itemNode.path("icms_situacao_tributaria").asText()
      );
      items.add(item);
    }
  }

  // Criar DTO principal
  return new InvoiceTaxDetails(
      rootNode.path("status").asText(),
      rootNode.path("numero").asText(),
      dataEmissao,
      new BigDecimal(requisicaoNode.path("valor_produtos").asText("0")),
      new BigDecimal(requisicaoNode.path("valor_total").asText("0")),
      new BigDecimal(requisicaoNode.path("icms_base_calculo").asText("0")),
      new BigDecimal(requisicaoNode.path("icms_valor_total").asText("0")),
      items,
      ref
  );
}
}
