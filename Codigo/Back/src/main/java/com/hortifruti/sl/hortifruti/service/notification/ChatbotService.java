package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.model.chatbot.ChatSession;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionContext;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionStatus;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import com.hortifruti.sl.hortifruti.service.chatbot.ChatSessionService;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceService;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pelo processamento de mensagens do chatbot WhatsApp.
 *
 * <p>Gerencia a interação com clientes através do WhatsApp, processando comandos relacionados a
 * consulta de boletos, solicitações de ajuda e saudações.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

  private final WhatsAppService whatsAppService;
  private final BilletService billetService;
  private final ClientRepository clientRepository;
  private final CombinedScoreRepository combinedScoreRepository;
  private final ChatSessionService chatSessionService;
  private final InvoiceService invoiceService;

  // Cache para rastrear mensagens enviadas pelo bot nos últimos 10 segundos
  // Key: phoneNumber, Value: timestamp da última mensagem enviada pelo bot
  private final Map<String, Long> botSentMessages = new java.util.concurrent.ConcurrentHashMap<>();
  private static final long BOT_MESSAGE_THRESHOLD_MS = 10000;

  private static final String CONTACT_PHONE = "(31) 3641-2244";

  /**
   * Processa mensagens recebidas do webhook do WhatsApp.
   *
   * <p>Extrai informações do payload, valida se é uma mensagem privada válida e encaminha para
   * processamento de comandos.
   *
   * <p>Detecta automaticamente mensagens manuais enviadas por atendentes e pausa o bot por 1 hora
   * para evitar conflitos.
   *
   * @param payload Dados recebidos do webhook contendo informações da mensagem
   */
  public void processIncomingMessage(Map<String, Object> payload) {
    try {
      Object dataObj = payload.get("data");
      if (!(dataObj instanceof Map)) {
        return;
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) dataObj;
      String from = (String) data.getOrDefault("from", "");
      String to = (String) data.getOrDefault("to", "");

      if (!from.endsWith("@c.us")) {
        return;
      }

      String messageBody = extractMessageBodyUltraMsg(data);
      String messageType = extractMessageTypeUltraMsg(data);

      if (!"chat".equals(messageType)) {
        return;
      }

      boolean isFromMe = detectIfMessageIsFromBot(data);

      String phoneNumber;
      if (isFromMe) {
        phoneNumber = extractPhoneFromJid(to);
      } else {
        phoneNumber = extractPhoneFromJid(from);
      }

      if (isFromMe) {
        Long lastBotMessageTime = botSentMessages.get(phoneNumber);
        long now = System.currentTimeMillis();

        if (lastBotMessageTime != null && (now - lastBotMessageTime) < BOT_MESSAGE_THRESHOLD_MS) {
          botSentMessages.remove(phoneNumber);
          return;
        }

        ChatSession session = chatSessionService.getOrCreateSession(phoneNumber);
        chatSessionService.pauseBotForSession(session.getId(), 1);
        chatSessionService.updateSessionStatus(session.getId(), SessionStatus.PAUSED);
        return;
      }

      processCommand(phoneNumber, messageBody);

    } catch (Exception e) {
      log.error("Erro ao processar mensagem do webhook");
    }
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
  private void processCommand(String phoneNumber, String message) {
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
      log.error("Erro ao processar comando do chatbot");
      sendErrorMessage(phoneNumber);
    }
  }

  private void handleMenuSelection(ChatSession session, String phoneNumber, String message) {
    String normalized = message.toLowerCase().trim();

    if (normalized.equals("1") || normalized.contains("pedido")) {
      chatSessionService.setSessionContext(session.getId(), SessionContext.PEDIDO);
      chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_HUMAN);
      String msg =
          "📋 *Fazer Pedido*\n\n"
              + "Por favor, envie a lista de produtos que deseja:\n"
              + "Nossa equipe vai receber seu pedido e responder em breve com disponibilidade e valores.\n\n"
              + "💡 Digite MENU para voltar ao início";
      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, msg);
      return;
    }

    if (normalized.equals("2") || normalized.contains("outro")) {
      chatSessionService.setSessionContext(session.getId(), SessionContext.OUTRO);
      chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_HUMAN);
      String msg =
          "💬 *Falar com Atendimento*\n\n"
              + "Por favor, descreva seu assunto ou dúvida:\n"
              + "Nossa equipe vai receber sua mensagem e responder em breve.\n\n"
              + "💡 Digite MENU para voltar ao início";
      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, msg);
      return;
    }

    if (normalized.equals("3") || normalized.contains("boleto")) {
      chatSessionService.setSessionContext(session.getId(), SessionContext.BOLETO);
      chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_DOCUMENT);
      String msg =
          "💰 *Consultar Boletos Pendentes*\n\n"
              + "Para consultar seus boletos, por favor, envie seu CPF *(apenas números)* ou CNPJ.\n\n"
              + "Exemplo: 12345678900 ou 12345678000190\n\n"
              + "💡 Digite MENU para voltar ao início";
      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, msg);
      return;
    }

    if (normalized.equals("4") || normalized.contains("nota fiscal") || normalized.contains("nf")) {
      chatSessionService.setSessionContext(session.getId(), SessionContext.NOTA_FISCAL);
      chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_DOCUMENT);
      String msg =
          "📄 *Consultar Nota Fiscal*\n\n"
              + "Por favor, envie o *número da nota fiscal* que deseja consultar.\n\n"
              + "Exemplo: 123456\n\n"
              + "💡 Digite MENU para voltar ao início";
      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, msg);
      return;
    }

    sendMainMenu(phoneNumber);
  }

  private void sendMainMenu(String phoneNumber) {
    registerBotMessage(phoneNumber);

    String menu =
        "Olá! Bem-vindo ao Hortifruti SL!\n\n"
            + "Como posso te ajudar hoje? Digite o número da opção:\n\n"
            + "*1* - 📋 Pedido - Fazer novo pedido\n"
            + "*2* - 💬 Outro assunto - Falar com atendimento\n"
            + "*3* - 💰 Boletos - Consultar boletos pendentes\n"
            + "*4* - 📄 Nota Fiscal - Consultar NF por número\n\n"
            + "Digite o número da opção desejada (1, 2, 3 ou 4)\n\n"
            + "💡 A qualquer momento, digite MENU para voltar aqui";
    whatsAppService.sendTextMessage(phoneNumber, menu);
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
        String msg =
            "❌ Documento inválido. Por favor, envie um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.\n\n"
                + "Exemplo: 12345678900 ou 12345678000190\n\n"
                + "💡 Digite MENU para voltar ao início";
        whatsAppService.sendTextMessage(phoneNumber, msg);
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
        String msg =
            "❌ Número da nota fiscal inválido.\n\n"
                + "Por favor, envie apenas números.\n"
                + "Exemplo: 123456\n\n"
                + "💡 Digite MENU para voltar ao início";
        whatsAppService.sendTextMessage(phoneNumber, msg);
        return;
      }

      String foundRef = findInvoiceRefByNumber(cleanNumber);

      if (foundRef == null) {
        String msg =
            "❌ Nota fiscal não encontrada.\n\n"
                + "Verifique se o número *"
                + cleanNumber
                + "* está correto.\n\n"
                + "💡 Digite MENU para voltar ao início ou entre em contato:\n"
                + "📞 "
                + CONTACT_PHONE;
        whatsAppService.sendTextMessage(phoneNumber, msg);
        chatSessionService.closeSession(session.getId(), "NOT_FOUND");
        return;
      }

      var invoiceResponse = invoiceService.consultInvoice(foundRef);

      if (invoiceResponse == null) {
        String msg =
            "❌ Erro ao consultar a nota fiscal.\n\n"
                + "Por favor, tente novamente ou entre em contato:\n"
                + "📞 "
                + CONTACT_PHONE
                + "\n\n"
                + "💡 Digite MENU para voltar ao início";
        whatsAppService.sendTextMessage(phoneNumber, msg);
        chatSessionService.closeSession(session.getId(), "ERROR");
        return;
      }

      StringBuilder messageBuilder = new StringBuilder();
      messageBuilder.append("📄 *Nota Fiscal Encontrada*\n\n");
      messageBuilder.append(String.format("*Número:* %s\n", invoiceResponse.number()));
      messageBuilder.append(String.format("*Status:* %s\n", invoiceResponse.status()));
      messageBuilder.append(
          String.format("*Valor Total:* R$ %.2f\n", invoiceResponse.totalValue()));
      messageBuilder.append(String.format("*Data:* %s\n", invoiceResponse.date()));
      messageBuilder.append(String.format("*Cliente:* %s\n\n", invoiceResponse.name()));

      if ("autorizado".equalsIgnoreCase(invoiceResponse.status())) {
        messageBuilder.append("✅ *Documento Disponível*\n\n");
        messageBuilder.append("Aguarde enquanto preparo o PDF da nota fiscal...");
        whatsAppService.sendTextMessage(phoneNumber, messageBuilder.toString());
        try {
          ResponseEntity<Resource> danfeResponse =
              invoiceService.downloadDanfe(invoiceResponse.reference());
          Resource resource = danfeResponse.getBody();

          if (resource != null) {
            byte[] danfePdf = resource.getContentAsByteArray();
            if (!(danfePdf != null && danfePdf.length > 0)) {
              whatsAppService.sendTextMessage(
                  phoneNumber,
                  "⚠️ Documento não disponível no momento. Entre em contato: " + CONTACT_PHONE);
            }
          } else {
            whatsAppService.sendTextMessage(
                phoneNumber,
                "⚠️ Documento não disponível no momento. Entre em contato: " + CONTACT_PHONE);
          }
        } catch (Exception ex) {
          whatsAppService.sendTextMessage(
              phoneNumber, "❌ Erro ao processar o documento. Entre em contato: " + CONTACT_PHONE);
        }
      } else {
        messageBuilder.append("⚠️ *Documento Indisponível*\n\n");
        messageBuilder.append("Esta nota fiscal não está autorizada para download.\n");
        messageBuilder.append("Status atual: ").append(invoiceResponse.status()).append("\n\n");
        messageBuilder.append("Para mais informações, entre em contato:\n");
        messageBuilder.append("📞 " + CONTACT_PHONE);
        whatsAppService.sendTextMessage(phoneNumber, messageBuilder.toString());
      }

      chatSessionService.closeSession(session.getId(), "COMPLETED");

    } catch (Exception e) {
      String msg =
          "❌ Erro ao consultar a nota fiscal.\n\n"
              + "Por favor, verifique o número e tente novamente ou entre em contato:\n"
              + "📞 "
              + CONTACT_PHONE;
      whatsAppService.sendTextMessage(phoneNumber, msg);
      chatSessionService.closeSession(session.getId(), "ERROR");
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
      List<CombinedScore> allScoresWithInvoice =
          combinedScoreRepository.findAll().stream()
              .filter(
                  cs ->
                      cs.isHasInvoice()
                          && cs.getInvoiceRef() != null
                          && !cs.getInvoiceRef().isEmpty())
              .toList();

      for (CombinedScore cs : allScoresWithInvoice) {
        String ref = cs.getInvoiceRef();
        try {

          var invoiceResponse = invoiceService.consultInvoice(ref);

          if (invoiceResponse != null && invoiceResponse.number() != null) {
            String nfNumber = invoiceResponse.number().replaceAll("[^0-9]", "");

            if (nfNumber.equals(invoiceNumber)) {
              return ref;
            }
          }
        } catch (Exception ex) {
        }
      }

      return null;

    } catch (Exception e) {
      log.error("Erro ao buscar referência da nota fiscal");
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
      Optional<Client> clientOpt = clientRepository.findByDocument(document);
      if (clientOpt.isEmpty()) {
        String message =
            "Desculpe, não encontrei nenhum cliente com esse documento em nosso sistema.\n\n"
                + "Verifique se o CPF ou CNPJ está correto ou entre em contato conosco:\n"
                + CONTACT_PHONE;
        registerBotMessage(phoneNumber);
        whatsAppService.sendTextMessage(phoneNumber, message);
        return;
      }

      Client client = clientOpt.get();

      List<CombinedScore> pendingWithBillet =
          billetService.findAllPendingWithBilletByClient(client.getId());

      List<CombinedScore> allPending = billetService.findAllPendingByClient(client.getId());

      if (allPending.isEmpty()) {
        String message =
            String.format(
                "Olá, %s!\n\n"
                    + "Boa notícia! Você não possui cobranças pendentes no momento.\n\n"
                    + "Se tiver alguma dúvida, entre em contato conosco:\n"
                    + CONTACT_PHONE,
                client.getClientName());
        registerBotMessage(phoneNumber);
        whatsAppService.sendTextMessage(phoneNumber, message);
        return;
      }

      StringBuilder messageBuilder = new StringBuilder();
      messageBuilder.append(String.format("Olá, %s!\n\n", client.getClientName()));

      int totalWithBillet = pendingWithBillet.size();
      int totalWithoutBillet = allPending.size() - pendingWithBillet.size();

      messageBuilder.append(String.format("📋 *Cobranças Pendentes:* %d\n\n", allPending.size()));

      int i = 1;
      for (CombinedScore cs : allPending) {
        messageBuilder.append(String.format("*Cobrança %d:*\n", i));
        messageBuilder.append(String.format("Valor: R$ %.2f\n", cs.getTotalValue()));
        messageBuilder.append(
            String.format(
                "Vencimento: %s\n",
                cs.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        if (cs.isHasBillet()) {
          messageBuilder.append(
              String.format(
                  "✓ Boleto: %s\n",
                  cs.getYourNumber() != null ? cs.getYourNumber() : "Disponível"));
        } else {
          messageBuilder.append("○ Boleto: Não emitido ainda\n");
        }

        if (i < allPending.size()) {
          messageBuilder.append("────────────────\n\n");
        }
        i++;
      }

      messageBuilder.append("\n────────────────\n\n");
      messageBuilder.append("📦 *Boletos Disponíveis:*\n");
      if (totalWithBillet > 0) {
        messageBuilder.append(String.format("✓ %d Boleto(s) para download\n", totalWithBillet));
      } else {
        messageBuilder.append("⚠️ Nenhum boleto disponível no momento\n");
      }

      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, messageBuilder.toString());

      if (pendingWithBillet.isEmpty()) {
        String noDocumentsMessage = "⚠️ *Boletos Pendentes de Emissão*\n\n";
        noDocumentsMessage +=
            String.format(
                "Você possui %d cobrança(s) sem boleto emitido ainda.\n\n", totalWithoutBillet);
        noDocumentsMessage +=
            "*Entre em contato para mais informações:*\n"
                + "📞 "
                + CONTACT_PHONE
                + "\n\n"
                + "Horário de atendimento:\n"
                + "• Segunda a Sábado, 7h às 20h\n"
                + "• Domingo, 7h às 12h";

        registerBotMessage(phoneNumber);
        whatsAppService.sendTextMessage(phoneNumber, noDocumentsMessage);

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
          log.error("Erro ao gerar PDF do boleto");
        }
      }

      chatSessionService.associateClient(session.getId(), client.getId());
      chatSessionService.closeSession(session.getId(), "COMPLETED");

    } catch (Exception e) {
      log.error("Erro ao processar solicitação de boletos");
      sendErrorMessage(phoneNumber);
    }
  }

  private void handleUnknownCommand(String phoneNumber) {
    String message =
        "Desculpe, não entendi sua solicitação.\n\n"
            + "Comandos disponíveis:\n"
            + "- 'boletos' - Ver cobranças em aberto\n"
            + "- 'ajuda' - Lista de comandos\n"
            + "- 'oi' - Saudação e boas-vindas\n\n"
            + "Tente usar uma dessas palavras-chave!\n\n"
            + "Para outras dúvidas: "
            + CONTACT_PHONE;

    whatsAppService.sendTextMessage(phoneNumber, message);
  }

  private void sendErrorMessage(String phoneNumber) {
    String message =
        "Ops! Ocorreu um erro temporário.\n\n"
            + "Por favor, tente novamente em alguns minutos ou entre em contato:\n\n"
            + CONTACT_PHONE
            + "\n"
            + "Segunda a Sexta, 8h às 18h";

    whatsAppService.sendTextMessage(phoneNumber, message);
  }

  /**
   * Extrai o número de telefone do JID do WhatsApp.
   *
   * <p>Remove o sufixo @c.us do identificador retornando apenas os dígitos.
   *
   * @param jid Identificador completo do WhatsApp (ex: 559999999999@c.us)
   * @return Número de telefone extraído
   */
  private String extractPhoneFromJid(String jid) {
    if (jid == null) return "";
    int at = jid.indexOf("@");
    if (at > 0) {
      return jid.substring(0, at);
    }
    return jid;
  }

  private String extractMessageBodyUltraMsg(Map<String, Object> data) {
    return (String) data.getOrDefault("body", "");
  }

  private String extractMessageTypeUltraMsg(Map<String, Object> data) {
    return (String) data.getOrDefault("type", "chat");
  }

  /**
   * Detecta se a mensagem foi enviada pelo próprio bot/atendente ou pelo cliente.
   *
   * <p>No payload do UltraMsg, mensagens enviadas pelo número conectado (bot ou atendente manual)
   * têm o campo "fromMe" = true. Mensagens recebidas de clientes têm "fromMe" = false ou ausente.
   *
   * @param data Mapa de dados contendo informações da mensagem
   * @return true se a mensagem foi enviada pelo bot/atendente, false se foi do cliente
   */
  private boolean detectIfMessageIsFromBot(Map<String, Object> data) {
    Object fromMeObj = data.get("fromMe");

    if (fromMeObj instanceof Boolean) {
      return (Boolean) fromMeObj;
    }

    if (fromMeObj instanceof String) {
      String fromMeStr = (String) fromMeObj;
      return "true".equalsIgnoreCase(fromMeStr) || "1".equals(fromMeStr);
    }

    Object fromMe2Obj = data.get("from_me");
    if (fromMe2Obj instanceof Boolean) {
      return (Boolean) fromMe2Obj;
    }

    if (fromMe2Obj instanceof String) {
      String fromMe2Str = (String) fromMe2Obj;
      return "true".equalsIgnoreCase(fromMe2Str) || "1".equals(fromMe2Str);
    }

    return false;
  }
}
