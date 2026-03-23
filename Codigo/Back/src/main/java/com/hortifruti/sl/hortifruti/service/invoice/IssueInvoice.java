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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


  private static final Logger log = LoggerFactory.getLogger(IssueInvoice.class);
  @Transactional
  public InvoiceResponse issueInvoice(Long combinedScoreId, String dadosAdicionais) {
    try {
      log.info("=== INICIANDO EMISSÃO DE NF - combinedScoreId: {} ===", combinedScoreId);

      CombinedScore combinedScore = fetchCombinedScore(combinedScoreId);
      log.info("CombinedScore encontrado: id={}, clientId={}", combinedScore.getId(), combinedScore.getClientId());

      Client client = fetchClient(combinedScore.getClientId());
      log.info("Cliente encontrado: id={}, nome={}", client.getId(), client.getClientName());

      RecipientRequest recipient = recipientService.createRecipientRequest(client.getId());
      log.info("Destinatário criado: {}", recipient);

      List<ItemRequest> items = invoiceItemService.createItems(
          combinedScore.getGroupedProducts(), recipient.endereco().uf());
      log.info("Itens criados: {} itens", items.size());
      items.forEach(item -> log.info("  Item: {}", item));

      IssueInvoiceRequest request = buildInvoiceRequest(recipient, items, combinedScore, dadosAdicionais);
      log.info("Request montado: {}", request);

      String ref = UUID.randomUUID().toString();
      String payload = invoicePayloadService.buildFocusNfePayload(request, ref);
      log.info("Payload gerado para ref {}: {}", ref, payload);

      String response = focusNfeApiClient.sendRequest(ref, payload);
      log.info("Resposta da FocusNFe: {}", response);

      InvoiceResponse invoiceResponse = objectMapper.readValue(response, InvoiceResponse.class);
      updateCombinedScoreStatus(combinedScore, invoiceResponse);

      log.info("=== NF EMITIDA COM SUCESSO - ref: {} ===", invoiceResponse.ref());
      return invoiceResponse;

    } catch (Exception e) {
      log.error("=== ERRO AO EMITIR NF - combinedScoreId: {} ===", combinedScoreId);
      log.error("Mensagem: {}", e.getMessage(), e);
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
      RecipientRequest recipient,
      List<ItemRequest> items,
      CombinedScore combinedScore,
      String dadosAdicionais) {
    Client client = fetchClient(combinedScore.getClientId());

    String firstName = client.getClientName().split("\\s+")[0].toUpperCase().trim();
    String infoText = info;

    if (firstName.contains("LLINEA")) {
      infoText = "Numerações AF: " + dadosAdicionais;
    }

    return new IssueInvoiceRequest(
        combinedScore.getId(),
        NATUREZA_OPERACAO,
        combinedScore
            .getConfirmedAt()
            .atTime(LocalTime.now(ZoneId.of("America/Sao_Paulo")))
            .atZone(ZoneId.of("America/Sao_Paulo"))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
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
    combinedScore.setHasInvoice(true);
    combinedScore.setInvoiceRef(invoiceResponse.ref());
    combinedScoreRepository.save(combinedScore);
  }
}
