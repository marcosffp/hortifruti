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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
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
    log.info("[IssueInvoice] Iniciando emissão de NF-e para combinedScoreId={}, dadosAdicionais={}",
        combinedScoreId, dadosAdicionais);
    try {
      CombinedScore combinedScore = fetchCombinedScore(combinedScoreId);
      log.info("[IssueInvoice] CombinedScore encontrado: id={}, clientId={}, confirmedAt={}, hasInvoice={}",
          combinedScore.getId(), combinedScore.getClientId(),
          combinedScore.getConfirmedAt(), combinedScore.isHasInvoice());

      Client client = fetchClient(combinedScore.getClientId());
      log.info("[IssueInvoice] Cliente encontrado: id={}, nome={}",
          client.getId(), client.getClientName());

      log.info("[IssueInvoice] Criando destinatário para clientId={}", client.getId());
      RecipientRequest recipient = recipientService.createRecipientRequest(client.getId());
      log.info("[IssueInvoice] Destinatário criado: cpfCnpj={}, uf={}",
          recipient.cpf_cnpj(), recipient.endereco() != null ? recipient.endereco().uf() : "NULL");

      log.info("[IssueInvoice] Criando itens da NF-e. Produtos agrupados: {}",
          combinedScore.getGroupedProducts());
      List<ItemRequest> items =
          invoiceItemService.createItems(
              combinedScore.getGroupedProducts(), recipient.endereco().uf());
      log.info("[IssueInvoice] {} item(s) criado(s)", items.size());
      items.forEach(item -> log.debug("[IssueInvoice] Item: {}", item));

      log.info("[IssueInvoice] Construindo payload da requisição...");
      IssueInvoiceRequest request =
          buildInvoiceRequest(recipient, items, combinedScore, dadosAdicionais);
      log.info("[IssueInvoice] IssueInvoiceRequest construído: {}", request);

      String ref = UUID.randomUUID().toString();
      log.info("[IssueInvoice] Referência UUID gerada: {}", ref);

      String payload = invoicePayloadService.buildFocusNfePayload(request, ref);
      log.info("[IssueInvoice] Payload Focus NF-e gerado:\n{}", payload);

      log.info("[IssueInvoice] Enviando requisição para Focus NF-e (ref={})...", ref);
      String response = focusNfeApiClient.sendRequest(ref, payload);
      log.info("[IssueInvoice] Resposta recebida da Focus NF-e:\n{}", response);

      InvoiceResponse invoiceResponse = objectMapper.readValue(response, InvoiceResponse.class);
      log.info("[IssueInvoice] InvoiceResponse deserializado: ref={}, status={}",
          invoiceResponse.ref(), invoiceResponse.status());

      updateCombinedScoreStatus(combinedScore, invoiceResponse);
      log.info("[IssueInvoice] NF-e emitida com sucesso. ref={}", invoiceResponse.ref());

      return invoiceResponse;
    } catch (InvoiceException e) {
      log.error("[IssueInvoice] Erro de negócio ao emitir NF-e para combinedScoreId={}: {}",
          combinedScoreId, e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      log.error("[IssueInvoice] Erro inesperado ao emitir NF-e para combinedScoreId={}: {}",
          combinedScoreId, e.getMessage(), e);
      throw new InvoiceException("Erro ao emitir nota fiscal: " + e.getMessage(), e);
    }
  }

  private CombinedScore fetchCombinedScore(Long combinedScoreId) {
    log.debug("[IssueInvoice] Buscando CombinedScore id={}", combinedScoreId);
    return combinedScoreRepository
        .findById(combinedScoreId)
        .orElseThrow(() -> {
          log.error("[IssueInvoice] CombinedScore não encontrado para id={}", combinedScoreId);
          return new InvoiceException("ID da compra não encontrado");
        });
  }

  private Client fetchClient(Long clientId) {
    log.debug("[IssueInvoice] Buscando Client id={}", clientId);
    return clientRepository
        .findById(clientId)
        .orElseThrow(() -> {
          log.error("[IssueInvoice] Client não encontrado para id={}", clientId);
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
    log.debug("[IssueInvoice] Primeiro nome do cliente: '{}'", firstName);

    String infoText = info;
    if (firstName.contains("LLINEA")) {
      infoText = "Numerações AF: " + dadosAdicionais;
      log.info("[IssueInvoice] Cliente LLINEA detectado. infoText ajustado para: '{}'", infoText);
    }

    String dataHoraEmissao = combinedScore
        .getConfirmedAt()
        .atTime(LocalTime.now(ZoneId.of("America/Sao_Paulo")))
        .atZone(ZoneId.of("America/Sao_Paulo"))
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    log.debug("[IssueInvoice] Data/hora de emissão calculada: {}", dataHoraEmissao);

    return new IssueInvoiceRequest(
        combinedScore.getId(),
        NATUREZA_OPERACAO,
        dataHoraEmissao,
        recipient,
        items,
        infoText);
  }

  private void updateCombinedScoreStatus(
      CombinedScore combinedScore, InvoiceResponse invoiceResponse) {
    log.info("[IssueInvoice] Atualizando status do CombinedScore id={} com ref={}",
        combinedScore.getId(), invoiceResponse.ref());
    combinedScore.setHasInvoice(true);
    combinedScore.setInvoiceRef(invoiceResponse.ref());
    combinedScoreRepository.save(combinedScore);
    log.info("[IssueInvoice] CombinedScore atualizado com sucesso.");
  }
}
