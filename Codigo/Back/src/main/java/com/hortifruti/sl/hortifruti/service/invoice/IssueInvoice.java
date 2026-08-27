package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class IssueInvoice {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final int COMPLETE = 1;

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
    // Trava a linha do agrupamento (SELECT ... FOR UPDATE) para serializar requisições
    // concorrentes: se duas chegarem juntas (ex: duplo clique), a segunda só prossegue depois
    // que a primeira já commitou hasInvoice=true, e cai no bloqueio de duplicidade abaixo.
    CombinedScore combinedScore = combinedScoreService.findByIdForUpdate(combinedScoreId);

    if (combinedScore.isHasInvoice()) {
      throw new InvoiceException(
          "Nota fiscal já foi emitida para este agrupamento (id " + combinedScoreId + ").");
    }

    String ref = UUID.randomUUID().toString();
    InvoiceResponse invoiceResponse;
    JsonNode responseNode;
    try {
      Client client = fetchClient(combinedScore.getClientId());

      RecipientRequest recipient = recipientService.createRecipientRequest(client.getId());

      List<ItemRequest> items =
          invoiceItemService.createItems(
              combinedScore.getGroupedProducts(), recipient.endereco().uf());

      IssueInvoiceRequest request =
          buildInvoiceRequest(recipient, items, combinedScore, dadosAdicionais);

      String payload = invoicePayloadService.buildFocusNfePayload(request, ref);

      String response = focusNfeApiClient.sendRequest(ref, payload);

      responseNode = objectMapper.readTree(response);
      invoiceResponse = objectMapper.treeToValue(responseNode, InvoiceResponse.class);
    } catch (Exception issueError) {
      // Não sabemos se a Focus NFe processou o POST antes da falha (ex: timeout esperando a
      // resposta) — como a ref é gerada por nós, sempre podemos consultá-la depois: se a Focus NFe
      // já a reconhece, a emissão de fato aconteceu e só a resposta se perdeu. Sem essa checagem, o
      // usuário veria erro e tentaria de novo, duplicando a NF.
      ReconciliationResult reconciled = tryReconcileAfterIssueFailure(ref, issueError);
      if (reconciled == null) {
        throw new InvoiceException(
            "Erro ao emitir nota fiscal: " + issueError.getMessage(), issueError);
      }
      invoiceResponse = reconciled.response();
      responseNode = reconciled.node();
    }

    // A Focus NFe às vezes já resolve a autorização (ou rejeição) da Sefaz na própria resposta
    // síncrona do POST, sem passar pelo "processando_autorizacao" assíncrono. Se não checarmos
    // isso aqui, marcamos hasInvoice=true para uma nota que a Sefaz já rejeitou — o agrupamento
    // fica travado mostrando "Ver NF" no front para uma NF que nunca existiu de fato.
    if (isRejectedStatus(invoiceResponse.status())) {
      throw new InvoiceException(buildRejectionMessage(responseNode, invoiceResponse.status()));
    }

    // A partir daqui a Focus NFe já confirmou que aceitou a NF — qualquer falha abaixo é só no
    // registro/pós-processamento local, NÃO significa que a NF não foi emitida. Por isso essas
    // falhas não são mais reembrulhadas como "erro ao emitir nota fiscal" (o que sugeriria ao
    // usuário que é seguro tentar de novo e arriscaria duplicar a NF na Focus NFe).
    try {
      updateCombinedScoreStatus(combinedScore, invoiceResponse);
    } catch (Exception persistError) {
      log.error(
          "CRÍTICO: NF emitida na Focus NFe (ref={}) mas falha ao registrar localmente para"
              + " combinedScoreId={} — requer reconciliação manual",
          invoiceResponse.ref(),
          combinedScoreId,
          persistError);
      throw new InvoiceException(
          "A nota fiscal FOI emitida com sucesso (ref="
              + invoiceResponse.ref()
              + "), mas houve uma falha ao registrar isso no sistema. NÃO gere uma nova nota"
              + " fiscal para este agrupamento — contate o suporte informando esta referência para"
              + " reconciliação manual.",
          persistError);
    }

    try {
      fiscalNoteXmlStorageService.triggerSaveAfterIssuance(invoiceResponse.ref());
    } catch (Exception xmlError) {
      log.warn(
          "Falha ao disparar salvamento do XML/DANFE para ref={} — não afeta a emissão, que já"
              + " foi confirmada e registrada",
          invoiceResponse.ref(),
          xmlError);
    }

    return invoiceResponse;
  }

  private record ReconciliationResult(InvoiceResponse response, JsonNode node) {}

  /**
   * Consulta a Focus NFe pela ref (gerada antes do POST, então sempre disponível) para confirmar se
   * a emissão que falhou no cliente na verdade foi processada do outro lado. Retorna {@code null}
   * se a reconciliação não confirmar nada — aí é seguro reportar a falha original ao usuário.
   */
  private ReconciliationResult tryReconcileAfterIssueFailure(String ref, Exception issueError) {
    try {
      String response = focusNfeApiClient.sendGetRequest(ref, COMPLETE);
      JsonNode node = objectMapper.readTree(response);
      InvoiceResponse reconciled = objectMapper.treeToValue(node, InvoiceResponse.class);
      if (reconciled.status() == null || reconciled.status().isBlank()) {
        return null;
      }
      log.warn(
          "Emissão de NF-e (ref={}) falhou no cliente ({}), mas reconciliação confirmou que a"
              + " Focus NFe já processou a requisição (status={}) — tratando como emitida para"
              + " evitar duplicidade",
          ref,
          issueError.getMessage(),
          reconciled.status());
      return new ReconciliationResult(reconciled, node);
    } catch (Exception reconcileError) {
      log.warn(
          "Não foi possível reconciliar a ref={} após falha na emissão — reportando a falha"
              + " original: {}",
          ref,
          reconcileError.getMessage());
      return null;
    }
  }

  private boolean isRejectedStatus(String status) {
    if (status == null) {
      return false;
    }
    String normalized = status.toLowerCase();
    return normalized.contains("erro")
        || normalized.contains("denegado")
        || normalized.contains("cancelado")
        || normalized.contains("rejeitado");
  }

  private String buildRejectionMessage(JsonNode responseNode, String status) {
    String mensagemSefaz = responseNode.path("mensagem_sefaz").asText(null);
    if (mensagemSefaz != null && !mensagemSefaz.isBlank()) {
      return "A Sefaz rejeitou a nota fiscal: " + mensagemSefaz;
    }
    return "A nota fiscal não foi autorizada pela Sefaz (status: " + status + ").";
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
