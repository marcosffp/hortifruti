package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.IssueInvoiceRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.RecipientRequest;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.service.invoice.factory.InvoiceItem;
import com.hortifruti.sl.hortifruti.service.invoice.factory.InvoicePayload;
import com.hortifruti.sl.hortifruti.service.invoice.factory.Recipient;
import jakarta.transaction.Transactional;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class IssueInvoice {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${focus.nfe.token}")
  private String focusNfeToken;

  @Value("${focus.nfe.api.url}")
  private String focusNfeApiUrl;

  @Value("${focus.nfe.cnpj.emitente}")
  private String focusNfeCnpjEmitente;

  private final String NATUREZA_OPERACAO = "Venda de Produtos Hortifrutigranjeiros";

  private final ClientRepository clientRepository;
  private final CombinedScoreRepository combinedScoreRepository;
  private final Recipient recipientService;
  private final InvoiceItem invoiceItemService;
  private final InvoicePayload invoicePayloadService;
  private final String info = "Venda de produtos hortifrutigranjeiros frescos";
  private final FocusNfeApiClient focusNfeApiClient;

  @Transactional
  public InvoiceResponse issueInvoice(Long combinedScoreId, String dadosAdicionais) {
    System.out.println("[IssueInvoice] Iniciando emissão. combinedScoreId=" + combinedScoreId + ", dadosAdicionais=" + dadosAdicionais);
    try {
      CombinedScore combinedScore = fetchCombinedScore(combinedScoreId);
      System.out.println("[IssueInvoice] CombinedScore encontrado: id=" + combinedScore.getId() + ", clientId=" + combinedScore.getClientId() + ", confirmedAt=" + combinedScore.getConfirmedAt() + ", hasInvoice=" + combinedScore.isHasInvoice());

      Client client = fetchClient(combinedScore.getClientId());
      System.out.println("[IssueInvoice] Cliente encontrado: id=" + client.getId() + ", nome=" + client.getClientName());

      System.out.println("[IssueInvoice] Criando destinatário para clientId=" + client.getId());
      RecipientRequest recipient = recipientService.createRecipientRequest(client.getId());
      System.out.println("[IssueInvoice] Destinatário criado: cpf_cnpj=" + recipient.cpf_cnpj() + ", uf=" + (recipient.endereco() != null ? recipient.endereco().uf() : "NULL"));

      System.out.println("[IssueInvoice] Produtos agrupados: " + combinedScore.getGroupedProducts());
      List<ItemRequest> items =
          invoiceItemService.createItems(
              combinedScore.getGroupedProducts(), recipient.endereco().uf());
      System.out.println("[IssueInvoice] Itens criados: " + items.size());
      items.forEach(item -> System.out.println("[IssueInvoice] Item: " + item));

      IssueInvoiceRequest request =
          buildInvoiceRequest(recipient, items, combinedScore, dadosAdicionais);
      System.out.println("[IssueInvoice] IssueInvoiceRequest: " + request);

      String ref = UUID.randomUUID().toString();
      System.out.println("[IssueInvoice] Ref gerada: " + ref);

      String payload = invoicePayloadService.buildFocusNfePayload(request, ref);
      System.out.println("[IssueInvoice] Payload enviado:\n" + payload);

      String response = focusNfeApiClient.sendRequest(ref, payload);
      System.out.println("[IssueInvoice] Resposta bruta da Focus NF-e:\n" + response);

      InvoiceResponse invoiceResponse = objectMapper.readValue(response, InvoiceResponse.class);
      System.out.println("[IssueInvoice] InvoiceResponse: ref=" + invoiceResponse.ref() + ", status=" + invoiceResponse.status());

      updateCombinedScoreStatus(combinedScore, invoiceResponse);
      System.out.println("[IssueInvoice] NF-e emitida com sucesso. ref=" + invoiceResponse.ref());

      return invoiceResponse;
    } catch (Exception e) {
      System.out.println("[IssueInvoice] ERRO: " + e.getClass().getName() + " - " + e.getMessage());
      e.printStackTrace();
      throw new InvoiceException("Erro ao emitir nota fiscal: " + e.getMessage(), e);
    }
  }

  private CombinedScore fetchCombinedScore(Long combinedScoreId) {
    System.out.println("[IssueInvoice] Buscando CombinedScore id=" + combinedScoreId);
    return combinedScoreRepository
        .findById(combinedScoreId)
        .orElseThrow(() -> {
          System.out.println("[IssueInvoice] CombinedScore NAO encontrado para id=" + combinedScoreId);
          return new InvoiceException("ID da compra não encontrado");
        });
  }

  private Client fetchClient(Long clientId) {
    System.out.println("[IssueInvoice] Buscando Client id=" + clientId);
    return clientRepository
        .findById(clientId)
        .orElseThrow(() -> {
          System.out.println("[IssueInvoice] Client NAO encontrado para id=" + clientId);
          return new InvoiceException("ID do cliente não encontrado");
        });
  }

  private IssueInvoiceRequest buildInvoiceRequest(
      RecipientRequest recipient,
      List<ItemRequest> items,
      CombinedScore combinedScore,
      String dadosAdicionais) {

    Client client = fetchClient(combinedScore.getClientId());
    String firstName = client.getClientName().split("\\s+")[0].toUpperCase().trim();
    System.out.println("[IssueInvoice] Primeiro nome: '" + firstName + "'");

    String infoText = info;
    if (firstName.contains("LLINEA")) {
      infoText = "Numerações AF: " + dadosAdicionais;
      System.out.println("[IssueInvoice] Cliente LLINEA detectado. infoText=" + infoText);
    }

    String dataHoraEmissao = combinedScore
        .getConfirmedAt()
        .atTime(LocalTime.now(ZoneId.of("America/Sao_Paulo")))
        .atZone(ZoneId.of("America/Sao_Paulo"))
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    System.out.println("[IssueInvoice] Data/hora emissão: " + dataHoraEmissao);

    return new IssueInvoiceRequest(
        combinedScore.getId(),
        NATUREZA_OPERACAO,
        dataHoraEmissao,
        recipient,
        items,
        infoText);
  }

  /*
   * private IssueInvoiceRequest buildInvoiceRequest(
   * Long combinedScoreId, RecipientRequest recipient, List<ItemRequest> items) {
   * return new IssueInvoiceRequest(
   * combinedScoreId,
   * NATUREZA_OPERACAO,
   * ZonedDateTime.of(2026, 1, 22, 0, 0, 0, 0, ZoneId.of("America/Sao_Paulo"))
   * .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
   * recipient,
   * items,
   * info);
   * }
   */

  private void updateCombinedScoreStatus(
      CombinedScore combinedScore, InvoiceResponse invoiceResponse) {
    System.out.println("[IssueInvoice] Atualizando CombinedScore id=" + combinedScore.getId() + " com ref=" + invoiceResponse.ref());
    combinedScore.setHasInvoice(true);
    combinedScore.setInvoiceRef(invoiceResponse.ref());
    combinedScoreRepository.save(combinedScore);
    System.out.println("[IssueInvoice] CombinedScore salvo com sucesso.");
  }
}
