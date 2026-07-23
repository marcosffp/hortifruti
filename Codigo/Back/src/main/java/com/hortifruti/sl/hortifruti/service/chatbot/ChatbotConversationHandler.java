package com.hortifruti.sl.hortifruti.service.chatbot;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.model.chatbot.ChatSession;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionContext;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionStatus;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceService;
import com.hortifruti.sl.hortifruti.service.notification.whatsapp.WhatsAppService;
import com.hortifruti.sl.hortifruti.service.purchase.ClientService;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Máquina de estado da conversa do chatbot: decide, a partir do status/contexto da {@link
 * ChatSession}, qual passo do fluxo executar a seguir. Persistência de sessão fica em {@link
 * ChatSessionService}; texto das mensagens fica em {@link ChatbotMessageTemplates}; parsing do
 * webhook fica em {@link WhatsAppWebhookParser}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatbotConversationHandler {

  private final WhatsAppService whatsAppService;
  private final BilletService billetService;
  private final ClientService clientService;
  private final CombinedScoreService combinedScoreService;
  private final ChatSessionService chatSessionService;
  private final InvoiceService invoiceService;

  // Cache para rastrear mensagens enviadas pelo bot nos últimos 10 segundos
  // Key: phoneNumber, Value: timestamp da última mensagem enviada pelo bot
  private final java.util.Map<String, Long> botSentMessages = new ConcurrentHashMap<>();
  private static final long BOT_MESSAGE_THRESHOLD_MS = 10000;

  /**
   * Trata o eco de uma mensagem enviada pelo próprio bot/atendente (fromMe=true) chegando de volta
   * pelo webhook. Se foi o bot que acabou de mandar (dentro do threshold), ignora; senão, é uma
   * resposta manual de atendente — pausa o bot para essa sessão por 1 hora.
   */
  public void handleBotEcho(String phoneNumber) {
    Long lastBotMessageTime = botSentMessages.get(phoneNumber);
    long now = System.currentTimeMillis();

    if (lastBotMessageTime != null && (now - lastBotMessageTime) < BOT_MESSAGE_THRESHOLD_MS) {
      botSentMessages.remove(phoneNumber);
      return;
    }

    ChatSession session = chatSessionService.getOrCreateSession(phoneNumber);
    chatSessionService.pauseBotForSession(session.getId(), 1);
    chatSessionService.updateSessionStatus(session.getId(), SessionStatus.PAUSED);
  }

  /**
   * Processa comandos do chatbot baseado na mensagem recebida.
   *
   * <p>Gerencia o fluxo completo da conversa através de sessões, incluindo: - Menu de opções -
   * Consulta de boletos por CPF/CNPJ - Encaminhamento para atendimento humano
   *
   * @param phoneNumber Número de telefone do remetente
   * @param message Conteúdo da mensagem enviada
   */
  public void processCommand(String phoneNumber, String message) {
    try {
      String normalized = message.toLowerCase().trim();
      if (normalized.equals("menu")
          || normalized.equals("recomeçar")
          || normalized.equals("recomecar")) {
        ChatSession session = chatSessionService.getOrCreateSession(phoneNumber);

        boolean wasPaused = chatSessionService.isBotPausedForPhone(phoneNumber);
        if (wasPaused) {
          chatSessionService.unpauseBot(session.getId());
        }

        chatSessionService.updateSessionStatus(session.getId(), SessionStatus.MENU);
        chatSessionService.setSessionContext(session.getId(), null);

        sendMainMenu(phoneNumber);
        return;
      }

      if (chatSessionService.isBotPausedForPhone(phoneNumber)) {
        return;
      }

      ChatSession session = chatSessionService.getOrCreateSession(phoneNumber);

      switch (session.getStatus()) {
        case MENU:
          handleMenuSelection(session, phoneNumber, message);
          break;

        case AWAITING_DOCUMENT:
          handleDocumentInput(session, phoneNumber, message);
          break;

        case AWAITING_HUMAN:
          break;

        case PAUSED:
          chatSessionService.updateSessionStatus(session.getId(), SessionStatus.MENU);
          sendMainMenu(phoneNumber);
          break;

        case CLOSED:
          session = chatSessionService.createNewSession(phoneNumber);
          sendMainMenu(phoneNumber);
          break;

        default:
          handleUnknownCommand(phoneNumber);
      }

    } catch (Exception e) {
      log.error("Erro ao processar comando do chatbot", e);
      sendErrorMessage(phoneNumber);
    }
  }

  private void handleMenuSelection(ChatSession session, String phoneNumber, String message) {
    String normalized = message.toLowerCase().trim();

    if (normalized.equals("1") || normalized.contains("pedido")) {
      chatSessionService.setSessionContext(session.getId(), SessionContext.PEDIDO);
      chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_HUMAN);
      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.pedidoPrompt());
      return;
    }

    if (normalized.equals("2") || normalized.contains("outro")) {
      chatSessionService.setSessionContext(session.getId(), SessionContext.OUTRO);
      chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_HUMAN);
      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.outroPrompt());
      return;
    }

    if (normalized.equals("3") || normalized.contains("boleto")) {
      chatSessionService.setSessionContext(session.getId(), SessionContext.BOLETO);
      chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_DOCUMENT);
      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.boletoPrompt());
      return;
    }

    if (normalized.equals("4") || normalized.contains("nota fiscal") || normalized.contains("nf")) {
      chatSessionService.setSessionContext(session.getId(), SessionContext.NOTA_FISCAL);
      chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_DOCUMENT);
      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.notaFiscalPrompt());
      return;
    }

    sendMainMenu(phoneNumber);
  }

  private void sendMainMenu(String phoneNumber) {
    registerBotMessage(phoneNumber);
    whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.mainMenu());
  }

  /** Registra que o bot está enviando uma mensagem para evitar pausar quando o webhook retornar */
  private void registerBotMessage(String phoneNumber) {
    botSentMessages.put(phoneNumber, System.currentTimeMillis());
  }

  /** Processa entrada de documento (CPF/CNPJ ou número de NF) */
  private void handleDocumentInput(ChatSession session, String phoneNumber, String message) {
    SessionContext context = session.getContext();

    if (context == SessionContext.NOTA_FISCAL) {
      handleInvoiceQuery(session, phoneNumber, message);
      return;
    }

    if (context == SessionContext.BOLETO) {
      String onlyDigits = message.replaceAll("[^0-9]", "");

      if (onlyDigits.length() == 11 || onlyDigits.length() == 14) {
        handleBilletRequestByDocument(session, phoneNumber, onlyDigits);
      } else {
        whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.invalidDocument());
      }
      return;
    }

    sendMainMenu(phoneNumber);
  }

  /**
   * Consulta e envia informações de uma nota fiscal específica pelo número
   *
   * <p>O cliente informa apenas o NÚMERO da nota fiscal (ex: 123456). O sistema busca a referência
   * (ref) correspondente no banco de dados e então consulta os detalhes na API Focus NFe.
   *
   * @param session Sessão de chat ativa
   * @param phoneNumber Número de telefone do cliente
   * @param invoiceNumber Número da nota fiscal informado pelo cliente
   */
  private void handleInvoiceQuery(ChatSession session, String phoneNumber, String invoiceNumber) {
    try {
      String cleanNumber = invoiceNumber.replaceAll("[^0-9]", "");

      if (cleanNumber.isEmpty()) {
        whatsAppService.sendTextMessage(
            phoneNumber, ChatbotMessageTemplates.invalidInvoiceNumber());
        return;
      }

      String foundRef = findInvoiceRefByNumber(cleanNumber);

      if (foundRef == null) {
        whatsAppService.sendTextMessage(
            phoneNumber, ChatbotMessageTemplates.invoiceNotFound(cleanNumber));
        chatSessionService.closeSession(session.getId(), "NOT_FOUND");
        return;
      }

      InvoiceResponseGet invoiceResponse = invoiceService.consultInvoice(foundRef);

      if (invoiceResponse == null) {
        whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.invoiceQueryFailed());
        chatSessionService.closeSession(session.getId(), "ERROR");
        return;
      }

      if ("autorizado".equalsIgnoreCase(invoiceResponse.status())) {
        whatsAppService.sendTextMessage(
            phoneNumber, ChatbotMessageTemplates.documentAvailableNotice(invoiceResponse));
        sendDanfeDocument(phoneNumber, invoiceResponse.reference());
      } else {
        whatsAppService.sendTextMessage(
            phoneNumber, ChatbotMessageTemplates.invoiceUnavailableStatus(invoiceResponse));
      }

      chatSessionService.closeSession(session.getId(), "COMPLETED");

    } catch (Exception e) {
      log.error("Erro ao consultar nota fiscal para o telefone {}", phoneNumber, e);
      whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.invoiceQueryException());
      chatSessionService.closeSession(session.getId(), "ERROR");
    }
  }

  private void sendDanfeDocument(String phoneNumber, String reference) {
    try {
      ResponseEntity<Resource> danfeResponse = invoiceService.downloadDanfe(reference);
      Resource resource = danfeResponse.getBody();

      byte[] danfePdf = resource != null ? resource.getContentAsByteArray() : null;
      if (danfePdf == null || danfePdf.length == 0) {
        whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.documentUnavailable());
      }
    } catch (Exception ex) {
      log.error("Erro ao baixar DANFE para referência {}", reference, ex);
      whatsAppService.sendTextMessage(
          phoneNumber, ChatbotMessageTemplates.documentProcessingError());
    }
  }

  /**
   * Busca a referência (ref) de uma nota fiscal pelo seu número.
   *
   * <p>Como o banco de dados não armazena o número da NF diretamente, este método busca todas as
   * refs de notas fiscais no banco e consulta cada uma na API até encontrar a que possui o número
   * informado.
   *
   * @param invoiceNumber Número da nota fiscal
   * @return Referência da nota fiscal ou null se não encontrada
   */
  private String findInvoiceRefByNumber(String invoiceNumber) {
    try {
      List<CombinedScore> allScoresWithInvoice = combinedScoreService.findAllWithInvoiceRef();

      for (CombinedScore cs : allScoresWithInvoice) {
        String ref = cs.getInvoiceRef();
        try {
          InvoiceResponseGet invoiceResponse = invoiceService.consultInvoice(ref);

          if (invoiceResponse != null && invoiceResponse.number() != null) {
            String nfNumber = invoiceResponse.number().replaceAll("[^0-9]", "");

            if (nfNumber.equals(invoiceNumber)) {
              return ref;
            }
          }
        } catch (Exception ex) {
          log.debug("Falha ao consultar nota fiscal com ref {} durante a busca", ref, ex);
        }
      }

      return null;

    } catch (Exception e) {
      log.error("Erro ao buscar referência da nota fiscal", e);
      return null;
    }
  }

  /**
   * Busca e envia boletos pendentes de um cliente específico.
   *
   * <p>Localiza o cliente pelo documento (CPF/CNPJ) e busca todos os combined scores pendentes que
   * possuem boletos emitidos (hasBillet = true).
   *
   * @param session Sessão de chat ativa
   * @param phoneNumber Número de telefone do cliente
   * @param document CPF ou CNPJ do cliente (apenas dígitos)
   */
  private void handleBilletRequestByDocument(
      ChatSession session, String phoneNumber, String document) {
    try {
      Optional<Client> clientOpt = clientService.findByDocument(document);
      if (clientOpt.isEmpty()) {
        registerBotMessage(phoneNumber);
        whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.clientNotFound());
        return;
      }

      Client client = clientOpt.get();

      List<CombinedScore> pendingWithBillet =
          billetService.findAllPendingWithBilletByClient(client.getId());

      List<CombinedScore> allPending = billetService.findAllPendingByClient(client.getId());

      if (allPending.isEmpty()) {
        registerBotMessage(phoneNumber);
        whatsAppService.sendTextMessage(
            phoneNumber, ChatbotMessageTemplates.noPendingCharges(client.getClientName()));
        return;
      }

      int totalWithBillet = pendingWithBillet.size();
      int totalWithoutBillet = allPending.size() - pendingWithBillet.size();

      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(
          phoneNumber,
          ChatbotMessageTemplates.pendingChargesMessage(
              client.getClientName(), allPending, totalWithBillet));

      if (pendingWithBillet.isEmpty()) {
        registerBotMessage(phoneNumber);
        whatsAppService.sendTextMessage(
            phoneNumber, ChatbotMessageTemplates.pendingBilletsNotice(totalWithoutBillet));

        chatSessionService.associateClient(session.getId(), client.getId());
        chatSessionService.closeSession(session.getId(), "COMPLETED");
        return;
      }

      List<byte[]> documents = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();
      for (int idx = 0; idx < pendingWithBillet.size(); idx++) {
        CombinedScore cs = pendingWithBillet.get(idx);
        try {
          ResponseEntity<byte[]> pdfResponse = billetService.issueCopy(cs.getId());
          byte[] pdf = pdfResponse.getBody();

          if (pdf != null && pdf.length > 0) {
            String fileName = "Boleto-" + cs.getId() + "-" + (idx + 1) + ".pdf";
            documents.add(pdf);
            fileNames.add(fileName);
          } else {
            log.warn("Boleto retornado vazio");
          }
        } catch (Exception ex) {
          log.error("Erro ao gerar PDF do boleto para CombinedScore {}", cs.getId(), ex);
        }
      }

      chatSessionService.associateClient(session.getId(), client.getId());
      chatSessionService.closeSession(session.getId(), "COMPLETED");

    } catch (Exception e) {
      log.error("Erro ao processar solicitação de boletos para o telefone {}", phoneNumber, e);
      sendErrorMessage(phoneNumber);
    }
  }

  private void handleUnknownCommand(String phoneNumber) {
    whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.unknownCommand());
  }

  private void sendErrorMessage(String phoneNumber) {
    whatsAppService.sendTextMessage(phoneNumber, ChatbotMessageTemplates.errorMessage());
  }
}
