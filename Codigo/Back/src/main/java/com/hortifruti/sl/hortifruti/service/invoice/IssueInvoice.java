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
import java.time.LocalDateTime;
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

  @Value("${focus.nfe.environment:homologacao}")
  private String focusNfeEnvironment;

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
  public InvoiceResponse issueInvoice(Long combinedScoreId) {
    try {
      CombinedScore combinedScore = fetchCombinedScore(combinedScoreId);
      Client client = fetchClient(combinedScore.getClientId());
      
      RecipientRequest recipient = recipientService.createRecipientRequest(client.getId());
      List<ItemRequest> items = invoiceItemService.createItems(combinedScore.getGroupedProducts());
      IssueInvoiceRequest request = buildInvoiceRequest(combinedScoreId, recipient, items);
      
      String ref = UUID.randomUUID().toString();
      String payload = invoicePayloadService.buildFocusNfePayload(request, ref);
      String response = focusNfeApiClient.sendRequest(ref, payload);
      
      InvoiceResponse invoiceResponse = objectMapper.readValue(response, InvoiceResponse.class);
      updateCombinedScoreStatus(combinedScore, invoiceResponse);

      return invoiceResponse;
    } catch (Exception e) {
      throw new InvoiceException("Erro ao emitir nota fiscal: " + e.getMessage(), e);
    }
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

  private IssueInvoiceRequest buildInvoiceRequest(
      Long combinedScoreId, RecipientRequest recipient, List<ItemRequest> items) {
    return new IssueInvoiceRequest(
        combinedScoreId,
        NATUREZA_OPERACAO,
        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        recipient,
        items,
        info);
  }

  private void updateCombinedScoreStatus(CombinedScore combinedScore, InvoiceResponse invoiceResponse) {
    combinedScore.setHasInvoice(true);
    combinedScore.setInvoiceRef(invoiceResponse.ref());
    combinedScoreRepository.save(combinedScore);
  }
}
