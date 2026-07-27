package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.IssueInvoiceRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.RecipientRequest;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.invoice.factory.InvoiceItem;
import com.hortifruti.sl.hortifruti.service.invoice.factory.InvoicePayload;
import com.hortifruti.sl.hortifruti.service.invoice.factory.Recipient;
import com.hortifruti.sl.hortifruti.service.purchase.ClientBusinessRules;
import com.hortifruti.sl.hortifruti.service.purchase.ClientService;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import jakarta.transaction.Transactional;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class IssueInvoice {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final String NATUREZA_OPERACAO = "Venda de Produtos Hortifrutigranjeiros";

  private final ClientService clientService;
  private final CombinedScoreService combinedScoreService;
  private final Recipient recipientService;
  private final InvoiceItem invoiceItemService;
  private final InvoicePayload invoicePayloadService;
  private final String info = "Venda de produtos hortifrutigranjeiros frescos";
  private final FocusNfeApiClient focusNfeApiClient;
  private final FiscalNoteXmlStorageService fiscalNoteXmlStorageService;

  @Transactional
  public InvoiceResponse issueInvoice(Long combinedScoreId, String dadosAdicionais) {
    try {
      // Trava a linha do agrupamento (SELECT ... FOR UPDATE) para serializar requisições
      // concorrentes: se duas chegarem juntas (ex: duplo clique), a segunda só prossegue depois
      // que a primeira já commitou hasInvoice=true, e cai no bloqueio de duplicidade abaixo.
      CombinedScore combinedScore = combinedScoreService.findByIdForUpdate(combinedScoreId);

      if (combinedScore.isHasInvoice()) {
        throw new InvoiceException(
            "Nota fiscal já foi emitida para este agrupamento (id " + combinedScoreId + ").");
      }

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

    Client client = fetchClient(combinedScore.getClientId());
    String firstName = client.getClientName().split("\\s+")[0].toUpperCase().trim();

    ClientBusinessRules.ClientRule clientRule = ClientBusinessRules.getRuleForCnpjClient(firstName);

    if (clientRule.isRequiresDadosAdicionais()
        && (dadosAdicionais == null || dadosAdicionais.isBlank())) {
      throw new InvoiceException(
          "As numerações dos pedidos são obrigatórias para gerar a NF deste cliente.");
    }

    String customNoteText = clientRule.buildInvoiceNoteText(dadosAdicionais);
    String infoText = customNoteText != null ? customNoteText : info;

    String dataHora =
        combinedScore
            .getConfirmedAt()
            .atTime(LocalTime.now(ZoneId.of("America/Sao_Paulo")))
            .atZone(ZoneId.of("America/Sao_Paulo"))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    return new IssueInvoiceRequest(
        combinedScore.getId(), NATUREZA_OPERACAO, dataHora, recipient, items, infoText);
  }

  private Client fetchClient(Long clientId) {
    return clientService.findById(clientId);
  }

  private void updateCombinedScoreStatus(
      CombinedScore combinedScore, InvoiceResponse invoiceResponse) {
    combinedScore.setHasInvoice(true);
    combinedScore.setInvoiceRef(invoiceResponse.ref());
    combinedScoreService.save(combinedScore);
  }
}
