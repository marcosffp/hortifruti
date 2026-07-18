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
  private final FiscalNoteXmlStorageService fiscalNoteXmlStorageService;

  @Transactional
  public InvoiceResponse issueInvoice(Long combinedScoreId, String dadosAdicionais) {
    try {
      CombinedScore combinedScore = fetchCombinedScore(combinedScoreId);

      Client client = fetchClient(combinedScore.getClientId());

      RecipientRequest recipient = recipientService.createRecipientRequest(client.getId());

      List<ItemRequest> items =
          invoiceItemService.createItems(
              combinedScore.getGroupedProducts(), recipient.endereco().uf());

      IssueInvoiceRequest request =
          buildInvoiceRequest(recipient, items, combinedScore, dadosAdicionais);

      String ref = UUID.randomUUID().toString();

      String payload = invoicePayloadService.buildFocusNfePayload(request, ref);

      String response = focusNfeApiClient.sendRequest(ref, payload);

      InvoiceResponse invoiceResponse = objectMapper.readValue(response, InvoiceResponse.class);

      updateCombinedScoreStatus(combinedScore, invoiceResponse);

      fiscalNoteXmlStorageService.triggerSaveAfterIssuance(invoiceResponse.ref());

      return invoiceResponse;
    } catch (Exception e) {
      throw new InvoiceException("Erro ao emitir nota fiscal: " + e.getMessage(), e);
    }
  }

  private IssueInvoiceRequest buildInvoiceRequest(
      RecipientRequest recipient,
      List<ItemRequest> items,
      CombinedScore combinedScore,
      String dadosAdicionais) {

    System.out.println(
        "[buildInvoiceRequest] Construindo request para clientId=" + combinedScore.getClientId());

    Client client = fetchClient(combinedScore.getClientId());
    String firstName = client.getClientName().split("\\s+")[0].toUpperCase().trim();
    System.out.println(
        "[buildInvoiceRequest] firstName=" + firstName + ", dadosAdicionais=" + dadosAdicionais);

    String infoText = info;
    if (firstName.contains("LLINEA")) {
      infoText = "Numerações AF: " + dadosAdicionais;
      System.out.println("[buildInvoiceRequest] Cliente LLINEA detectado, infoText=" + infoText);
    }

    String dataHora =
        combinedScore
            .getConfirmedAt()
            .atTime(LocalTime.now(ZoneId.of("America/Sao_Paulo")))
            .atZone(ZoneId.of("America/Sao_Paulo"))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    System.out.println("[buildInvoiceRequest] dataHora da NF=" + dataHora);

    return new IssueInvoiceRequest(
        combinedScore.getId(), NATUREZA_OPERACAO, dataHora, recipient, items, infoText);
  }

  private CombinedScore fetchCombinedScore(Long combinedScoreId) {
    return combinedScoreRepository
        .findById(combinedScoreId)
        .orElseThrow(() -> new InvoiceException("ID da compra não encontrado"));
  }

  private Client fetchClient(Long clientId) {
    return clientRepository
        .findById(clientId)
        .orElseThrow(() -> new InvoiceException("ID do cliente não encontrado"));
  }

  private void updateCombinedScoreStatus(
      CombinedScore combinedScore, InvoiceResponse invoiceResponse) {
    combinedScore.setHasInvoice(true);
    combinedScore.setInvoiceRef(invoiceResponse.ref());
    combinedScoreRepository.save(combinedScore);
  }
}
