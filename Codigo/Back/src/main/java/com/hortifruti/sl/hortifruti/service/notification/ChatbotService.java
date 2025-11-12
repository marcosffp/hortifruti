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
@Service
@RequiredArgsConstructor
@Slf4j
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
  private static final long BOT_MESSAGE_THRESHOLD_MS = 10000; // 10 segundos
  
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

      Map<String, Object> data = (Map<String, Object>) dataObj;
      String from = (String) data.getOrDefault("from", "");
      String to = (String) data.getOrDefault("to", "");

      log.info("=== DEBUG WEBHOOK ===");
      log.info("FROM: {}", from);
      log.info("TO: {}", to);
      log.info("====================");

      if (!from.endsWith("@c.us")) {
        return;
      }

      String messageBody = extractMessageBodyUltraMsg(data);
      String messageType = extractMessageTypeUltraMsg(data);

      if (!"chat".equals(messageType)) {
        return;
      }

      // Detecta se a mensagem foi enviada manualmente pelo atendente
      boolean isFromMe = detectIfMessageIsFromBot(data);

      // Define o número correto do CLIENTE baseado em quem enviou a mensagem
      String phoneNumber;
      if (isFromMe) {
        // Mensagem enviada PELO ATENDENTE (você)
        // from = você (atendente) - 557799012005
        // to = cliente (destinatário) - 553398139500
        // Queremos o número do CLIENTE, então usamos TO
        phoneNumber = extractPhoneFromJid(to);
        log.info("Mensagem DO ATENDENTE PARA o cliente: {}", phoneNumber);
      } else {
        // Mensagem enviada PELO CLIENTE
        // from = cliente (remetente) - 553398139500
        // to = você (atendente) - 557799012005
        // Queremos o número do CLIENTE, então usamos FROM
        phoneNumber = extractPhoneFromJid(from);
        log.info("Mensagem DO CLIENTE: {}", phoneNumber);
      }

      if (isFromMe) {
        // Mensagem enviada pelo atendente ou pelo próprio bot
        log.info("Detectada mensagem fromMe=true para cliente: {}", phoneNumber);
        
        // Verifica se esta mensagem foi enviada pelo bot recentemente
        Long lastBotMessageTime = botSentMessages.get(phoneNumber);
        long now = System.currentTimeMillis();
        
        if (lastBotMessageTime != null && (now - lastBotMessageTime) < BOT_MESSAGE_THRESHOLD_MS) {
          // Mensagem enviada pelo bot automaticamente nos últimos 10 segundos
          log.info("Mensagem automática do BOT detectada para {}. Ignorando para não pausar.", phoneNumber);
          
          // Remove do cache após usar
          botSentMessages.remove(phoneNumber);
          return;
        }
        
        // Mensagem enviada manualmente pelo atendente via WhatsApp
        log.info(
            "Mensagem MANUAL do atendente detectada para cliente {}. Criando/obtendo sessão e pausando bot por 1 hora.",
            phoneNumber);

        // 1. Primeiro: Garante que a sessão existe (cria se necessário)
        ChatSession session = chatSessionService.getOrCreateSession(phoneNumber);
        log.info("Sessão obtida/criada: ID {}", session.getId());

        // 2. Depois: Pausa o bot para esta sessão
        chatSessionService.pauseBotForSession(session.getId(), 1);

        // 3. Por fim: Muda o status para PAUSED (atendimento humano em andamento)
        chatSessionService.updateSessionStatus(session.getId(), SessionStatus.PAUSED);
        log.info("Status da sessão {} alterado para PAUSED. Bot pausado até: {}", 
            session.getId(), session.getPausedUntil());

        return; // Não processa como comando
      }

      // Mensagem do cliente - processa normalmente
      processCommand(phoneNumber, messageBody);

    } catch (Exception e) {
      log.error("Erro ao processar mensagem recebida: {}", e.getMessage(), e);
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
      // 1. Verificar comandos globais ANTES de verificar se está pausado
      // (comandos como "menu" funcionam sempre, mesmo com bot pausado)
      String normalized = message.toLowerCase().trim();
      if (normalized.equals("menu")
          || normalized.equals("recomeçar")
          || normalized.equals("recomecar")) {
        // Obtém ou cria a sessão
        ChatSession session = chatSessionService.getOrCreateSession(phoneNumber);
        
        // Se estava pausado, remove a pausa
        boolean wasPaused = chatSessionService.isBotPausedForPhone(phoneNumber);
        if (wasPaused) {
          log.info("Cliente {} solicitou menu enquanto bot estava pausado. Despausando bot...", phoneNumber);
          chatSessionService.unpauseBot(session.getId());
          log.info("Bot despausado com sucesso para cliente {}", phoneNumber);
        }
        
        // Atualiza status para MENU e limpa contexto
        chatSessionService.updateSessionStatus(session.getId(), SessionStatus.MENU);
        chatSessionService.setSessionContext(session.getId(), null);
        
        log.info("Cliente {} no menu principal. Status: MENU, Pausado: false", phoneNumber);
        sendMainMenu(phoneNumber);
        return;
      }

      // 2. Verificar se o bot está pausado para este número
      if (chatSessionService.isBotPausedForPhone(phoneNumber)) {
        log.info("Bot pausado para telefone {}. Mensagem ignorada.", phoneNumber);
        return;
      }

      // 3. Obter ou criar sessão
      ChatSession session = chatSessionService.getOrCreateSession(phoneNumber);

      // 4. Processar baseado no status da sessão
      switch (session.getStatus()) {
        case MENU:
          handleMenuSelection(session, phoneNumber, message);
          break;

        case AWAITING_DOCUMENT:
          handleDocumentInput(session, phoneNumber, message);
          break;

        case AWAITING_HUMAN:
          // Cliente já está aguardando atendimento
          log.info("Cliente {} aguardando atendimento humano.", phoneNumber);
          break;

        case PAUSED:
          // Bot pausado - atendimento humano em andamento
          // NOTA: Normalmente nunca chegamos aqui porque verificamos isBotPausedForPhone antes
          // Mas se chegamos, é porque a pausa expirou mas o status ainda não foi atualizado
          log.warn("Status PAUSED detectado mas bot não está pausado. Mudando para MENU.");
          chatSessionService.updateSessionStatus(session.getId(), SessionStatus.MENU);
          sendMainMenu(phoneNumber);
          break;

        case CLOSED:
          // Sessão fechada, cria nova e mostra menu
          session = chatSessionService.createNewSession(phoneNumber);
          sendMainMenu(phoneNumber);
          break;

        default:
          handleUnknownCommand(phoneNumber);
      }

    } catch (Exception e) {
      log.error("Erro ao processar comando para {}: {}", phoneNumber, e.getMessage(), e);
      sendErrorMessage(phoneNumber);
    }
  }

  /** Processa a seleção do menu principal */
  private void handleMenuSelection(ChatSession session, String phoneNumber, String message) {
    String normalized = message.toLowerCase().trim();

    // Opção 1: Pedido
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

    // Opção 2: Outro assunto
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

    // Opção 3: Boletos
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

    // Opção 4: Nota Fiscal
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

    // Opção não reconhecida, reenvia menu
    sendMainMenu(phoneNumber);
  }

  /** Envia o menu principal */
  private void sendMainMenu(String phoneNumber) {
    // Registra que o bot vai enviar uma mensagem
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
  
  /**
   * Registra que o bot está enviando uma mensagem para evitar pausar quando o webhook retornar
   */
  private void registerBotMessage(String phoneNumber) {
    botSentMessages.put(phoneNumber, System.currentTimeMillis());
    log.debug("Registrada mensagem do bot para {}", phoneNumber);
  }

  /** Processa entrada de documento (CPF/CNPJ ou número de NF) */
  private void handleDocumentInput(ChatSession session, String phoneNumber, String message) {
    SessionContext context = session.getContext();

    // Se o contexto for NOTA_FISCAL, processa como número de NF
    if (context == SessionContext.NOTA_FISCAL) {
      handleInvoiceQuery(session, phoneNumber, message);
      return;
    }

    // Se o contexto for BOLETO, processa como CPF/CNPJ
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

    // Contexto desconhecido
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
      log.info("========================================");
      log.info("Consultando nota fiscal por NÚMERO: {}", invoiceNumber);
      log.info("Telefone: {}", phoneNumber);

      // Remove caracteres não numéricos
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

      log.info("Número limpo: {}", cleanNumber);

      // Busca a ref no banco de dados pelo número da nota fiscal
      // Como não temos o número armazenado, vamos buscar todas as refs
      // e consultar cada uma até encontrar o número correspondente
      log.info("Buscando referência da nota fiscal no banco de dados...");

      String foundRef = findInvoiceRefByNumber(cleanNumber);

      if (foundRef == null) {
        String msg =
            "❌ Nota fiscal não encontrada.\n\n"
                + "Verifique se o número *"
                + cleanNumber
                + "* está correto.\n\n"
                + "💡 Digite MENU para voltar ao início ou entre em contato:\n"
                + "📞 " + CONTACT_PHONE;
        whatsAppService.sendTextMessage(phoneNumber, msg);
        chatSessionService.closeSession(session.getId(), "NOT_FOUND");
        return;
      }

      log.info("✓ Referência encontrada: {}", foundRef);

      // Consultar a nota fiscal usando a ref encontrada
      var invoiceResponse = invoiceService.consultInvoice(foundRef);

      if (invoiceResponse == null) {
        String msg =
            "❌ Erro ao consultar a nota fiscal.\n\n"
                + "Por favor, tente novamente ou entre em contato:\n"
                + "📞 " + CONTACT_PHONE + "\n\n"
                + "💡 Digite MENU para voltar ao início";
        whatsAppService.sendTextMessage(phoneNumber, msg);
        chatSessionService.closeSession(session.getId(), "ERROR");
        return;
      }

      log.info("✓ Nota fiscal encontrada:");
      log.info("  Nome: {}", invoiceResponse.name());
      log.info("  Número: {}", invoiceResponse.number());
      log.info("  Status: {}", invoiceResponse.status());
      log.info("  Valor: R$ {}", invoiceResponse.totalValue());
      log.info("  Data: {}", invoiceResponse.date());
      log.info("  Referência: {}", invoiceResponse.reference());

      // Montar mensagem com informações da NF
      StringBuilder messageBuilder = new StringBuilder();
      messageBuilder.append("📄 *Nota Fiscal Encontrada*\n\n");
      messageBuilder.append(String.format("*Número:* %s\n", invoiceResponse.number()));
      messageBuilder.append(String.format("*Status:* %s\n", invoiceResponse.status()));
      messageBuilder.append(
          String.format("*Valor Total:* R$ %.2f\n", invoiceResponse.totalValue()));
      messageBuilder.append(String.format("*Data:* %s\n", invoiceResponse.date()));
      messageBuilder.append(String.format("*Cliente:* %s\n\n", invoiceResponse.name()));

      // Se a NF estiver autorizada, oferece download do PDF
      if ("autorizado".equalsIgnoreCase(invoiceResponse.status())) {
        messageBuilder.append("✅ *Documento Disponível*\n\n");
        messageBuilder.append("Aguarde enquanto preparo o PDF da nota fiscal...");
        whatsAppService.sendTextMessage(phoneNumber, messageBuilder.toString());

        // Baixar e enviar o DANFE
        log.info("Baixando DANFE para ref: {}", invoiceResponse.reference());
        try {
          ResponseEntity<Resource> danfeResponse =
              invoiceService.downloadDanfe(invoiceResponse.reference());
          Resource resource = danfeResponse.getBody();

          if (resource != null) {
            byte[] danfePdf = resource.getContentAsByteArray();
            if (danfePdf != null && danfePdf.length > 0) {
              String fileName = "NotaFiscal-" + invoiceResponse.number() + ".pdf";
              boolean sent =
                  whatsAppService.sendDocument(
                      phoneNumber,
                      "📄 Nota Fiscal nº " + invoiceResponse.number(),
                      danfePdf,
                      fileName);

              if (sent) {
                log.info("✓ DANFE enviado com sucesso!");
              } else {
                log.error("✗ Falha ao enviar DANFE");
                whatsAppService.sendTextMessage(
                    phoneNumber,
                    "⚠️ Houve um problema ao enviar o documento. Por favor, tente novamente.");
              }
            } else {
              log.warn("DANFE retornado é nulo ou vazio");
              whatsAppService.sendTextMessage(
                  phoneNumber,
                  "⚠️ Documento não disponível no momento. Entre em contato: " + CONTACT_PHONE);
            }
          } else {
            log.warn("Resource DANFE é nulo");
            whatsAppService.sendTextMessage(
                phoneNumber,
                "⚠️ Documento não disponível no momento. Entre em contato: " + CONTACT_PHONE);
          }
        } catch (Exception ex) {
          log.error("Erro ao baixar DANFE: {}", ex.getMessage(), ex);
          whatsAppService.sendTextMessage(
              phoneNumber, "❌ Erro ao processar o documento. Entre em contato: " + CONTACT_PHONE);
        }
      } else {
        // NF não autorizada
        messageBuilder.append("⚠️ *Documento Indisponível*\n\n");
        messageBuilder.append("Esta nota fiscal não está autorizada para download.\n");
        messageBuilder.append("Status atual: ").append(invoiceResponse.status()).append("\n\n");
        messageBuilder.append("Para mais informações, entre em contato:\n");
        messageBuilder.append("📞 " + CONTACT_PHONE);
        whatsAppService.sendTextMessage(phoneNumber, messageBuilder.toString());
      }

      // Fechar sessão
      chatSessionService.closeSession(session.getId(), "COMPLETED");
      log.info("Sessão {} finalizada", session.getId());
      log.info("========================================");

    } catch (Exception e) {
      log.error(
          "Erro ao consultar nota fiscal com número {}: {}", invoiceNumber, e.getMessage(), e);
      String msg =
          "❌ Erro ao consultar a nota fiscal.\n\n"
              + "Por favor, verifique o número e tente novamente ou entre em contato:\n"
              + "📞 " + CONTACT_PHONE;
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
      // Busca todas as refs de notas fiscais no banco
      log.info("Buscando todas as referências de notas fiscais no banco...");
      List<CombinedScore> allScoresWithInvoice =
          combinedScoreRepository.findAll().stream()
              .filter(
                  cs ->
                      cs.isHasInvoice()
                          && cs.getInvoiceRef() != null
                          && !cs.getInvoiceRef().isEmpty())
              .toList();

      log.info("Total de CombinedScores com nota fiscal: {}", allScoresWithInvoice.size());

      // Para cada ref, consulta na API e verifica se o número corresponde
      for (CombinedScore cs : allScoresWithInvoice) {
        String ref = cs.getInvoiceRef();
        try {
          log.info("Verificando ref: {}", ref);
          var invoiceResponse = invoiceService.consultInvoice(ref);

          if (invoiceResponse != null && invoiceResponse.number() != null) {
            String nfNumber = invoiceResponse.number().replaceAll("[^0-9]", "");
            log.info("  Número da NF: {} (comparando com {})", nfNumber, invoiceNumber);

            if (nfNumber.equals(invoiceNumber)) {
              log.info("✓ Nota fiscal encontrada! Ref: {}", ref);
              return ref;
            }
          }
        } catch (Exception ex) {
          log.warn("Erro ao consultar ref {}: {}", ref, ex.getMessage());
          // Continua para a próxima ref
        }
      }

      log.warn("Nota fiscal com número {} não encontrada", invoiceNumber);
      return null;

    } catch (Exception e) {
      log.error("Erro ao buscar referência da nota fiscal: {}", e.getMessage(), e);
      return null;
    }
  }

  /**
   * Busca e envia boletos pendentes de um cliente específico.
   *
   * <p>Localiza o cliente pelo documento (CPF/CNPJ) e busca todos os combined scores pendentes
   * que possuem boletos emitidos (hasBillet = true).
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
      log.info("========================================");
      log.info("Cliente encontrado: {} (ID: {})", client.getClientName(), client.getId());
      log.info("Documento: {}", document);

      // Busca Combined Scores pendentes COM BOLETO (hasBillet = true)
      List<CombinedScore> pendingWithBillet =
          billetService.findAllPendingWithBilletByClient(client.getId());
      log.info("Combined Scores pendentes COM BOLETO: {}", pendingWithBillet.size());

      // Busca TODOS os Combined Scores pendentes (para informar ao cliente)
      List<CombinedScore> allPending = billetService.findAllPendingByClient(client.getId());
      log.info("Combined Scores pendentes TOTAL: {}", allPending.size());

      // Se não houver cobranças pendentes
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

      // Monta mensagem resumo
      StringBuilder messageBuilder = new StringBuilder();
      messageBuilder.append(String.format("Olá, %s!\n\n", client.getClientName()));

      // Contadores
      int totalWithBillet = pendingWithBillet.size();
      int totalWithoutBillet = allPending.size() - pendingWithBillet.size();

      // Informações sobre cobranças pendentes
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

      // Resumo de documentos disponíveis
      messageBuilder.append("\n────────────────\n\n");
      messageBuilder.append("📦 *Boletos Disponíveis:*\n");
      if (totalWithBillet > 0) {
        messageBuilder.append(String.format("✓ %d Boleto(s) para download\n", totalWithBillet));
      } else {
        messageBuilder.append("⚠️ Nenhum boleto disponível no momento\n");
      }

      log.info(
          "Resumo - Cobranças: {}, Com boleto: {}, Sem boleto: {}",
          allPending.size(),
          totalWithBillet,
          totalWithoutBillet);

      registerBotMessage(phoneNumber);
      whatsAppService.sendTextMessage(phoneNumber, messageBuilder.toString());

      // Se não houver boletos emitidos, não precisa continuar
      if (pendingWithBillet.isEmpty()) {
        String noDocumentsMessage = "⚠️ *Boletos Pendentes de Emissão*\n\n";
        noDocumentsMessage +=
            String.format(
                "Você possui %d cobrança(s) sem boleto emitido ainda.\n\n", totalWithoutBillet);
        noDocumentsMessage +=
            "*Entre em contato para mais informações:*\n"
                + "📞 " + CONTACT_PHONE + "\n\n"
                + "Horário de atendimento:\n"
                + "• Segunda a Sábado, 7h às 20h\n"
                + "• Domingo, 7h às 12h";
        
        registerBotMessage(phoneNumber);
        whatsAppService.sendTextMessage(phoneNumber, noDocumentsMessage);
        
        // Associar cliente à sessão e finalizar
        chatSessionService.associateClient(session.getId(), client.getId());
        chatSessionService.closeSession(session.getId(), "COMPLETED");
        log.info("Sessão {} finalizada para cliente {}", session.getId(), client.getId());
        return;
      }

      // Listas para armazenar os boletos
      List<byte[]> documents = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();

      log.info("========================================");
      log.info("Iniciando coleta de boletos...");
      log.info("Combined Scores COM BOLETO a processar: {}", pendingWithBillet.size());

      // Processar APENAS boletos dos Combined Scores que têm hasBillet = true
      log.info("Processando {} boletos...", pendingWithBillet.size());
      int boletosAdicionados = 0;
      for (int idx = 0; idx < pendingWithBillet.size(); idx++) {
        CombinedScore cs = pendingWithBillet.get(idx);
        try {
          log.info(
              "  → [{}/{}] Obtendo boleto para CombinedScore ID: {} (YourNumber: {})",
              idx + 1,
              pendingWithBillet.size(),
              cs.getId(),
              cs.getYourNumber());
          ResponseEntity<byte[]> pdfResponse = billetService.issueCopy(cs.getId());
          byte[] pdf = pdfResponse.getBody();

          if (pdf != null && pdf.length > 0) {
            String fileName = "Boleto-" + cs.getId() + "-" + (idx + 1) + ".pdf";
            documents.add(pdf);
            fileNames.add(fileName);
            boletosAdicionados++;
            log.info(
                "    ✓ Boleto adicionado: {} ({} bytes) - Total: {}/{}",
                fileName,
                pdf.length,
                boletosAdicionados,
                pendingWithBillet.size());
          } else {
            log.warn("    ✗ Boleto retornado é nulo ou vazio para ID: {}", cs.getId());
          }
        } catch (Exception ex) {
          log.error(
              "    ✗ Falha ao gerar PDF do boleto para ID {}: {}", cs.getId(), ex.getMessage(), ex);
        }
      }
      log.info("Total de boletos adicionados: {}/{}", boletosAdicionados, pendingWithBillet.size());

      log.info("Coleta finalizada:");
      log.info("  • Boletos: {}/{}", boletosAdicionados, pendingWithBillet.size());
      log.info("  • Total de documentos coletados: {}", documents.size());
      log.info("========================================");

      // Enviar boletos
      if (!documents.isEmpty()) {
        int totalDocs = documents.size();
        String caption = String.format("📎 Enviando %d boleto(s)", totalDocs);

        log.info("========================================");
        log.info("PREPARANDO ENVIO DE BOLETOS");
        log.info("Destinatário: {}", phoneNumber);
        log.info("Total de boletos a enviar: {}", totalDocs);
        log.info("========================================");

        boolean sent =
            whatsAppService.sendMultipleDocuments(phoneNumber, caption, documents, fileNames);

        if (sent) {
          log.info("✓ SUCESSO: Todos os {} boletos foram enviados com sucesso!", totalDocs);
        } else {
          log.error("✗ FALHA: Um ou mais boletos não foram enviados corretamente");
        }
      }

      // Associar cliente à sessão
      chatSessionService.associateClient(session.getId(), client.getId());

      // Deleta a sessão após enviar os documentos
      chatSessionService.closeSession(session.getId(), "COMPLETED");
      log.info("Sessão {} finalizada para cliente {}", session.getId(), client.getId());

    } catch (Exception e) {
      log.error(
          "Erro ao processar solicitação de boletos para {}: {}",
          phoneNumber,
          e.getMessage(),
          e);
      sendErrorMessage(phoneNumber);
    }
  }

  /**
   * Envia mensagem informando que o comando não foi reconhecido e lista os comandos disponíveis.
   *
   * @param phoneNumber Número de telefone do destinatário
   */
  private void handleUnknownCommand(String phoneNumber) {
    String message =
        "Desculpe, não entendi sua solicitação.\n\n"
            + "Comandos disponíveis:\n"
            + "- 'boletos' - Ver cobranças em aberto\n"
            + "- 'ajuda' - Lista de comandos\n"
            + "- 'oi' - Saudação e boas-vindas\n\n"
            + "Tente usar uma dessas palavras-chave!\n\n"
            + "Para outras dúvidas: " + CONTACT_PHONE;

    whatsAppService.sendTextMessage(phoneNumber, message);
  }

  /**
   * Envia mensagem genérica de erro ao cliente.
   *
   * @param phoneNumber Número de telefone do destinatário
   */
  private void sendErrorMessage(String phoneNumber) {
    String message =
        "Ops! Ocorreu um erro temporário.\n\n"
            + "Por favor, tente novamente em alguns minutos ou entre em contato:\n\n"
            + CONTACT_PHONE + "\n"
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

  /**
   * Extrai o corpo da mensagem do payload do UltraMsg.
   *
   * @param data Mapa de dados contendo informações da mensagem
   * @return Conteúdo textual da mensagem
   */
  private String extractMessageBodyUltraMsg(Map<String, Object> data) {
    return (String) data.getOrDefault("body", "");
  }

  /**
   * Extrai o tipo da mensagem do payload do UltraMsg.
   *
   * @param data Mapa de dados contendo informações da mensagem
   * @return Tipo da mensagem (padrão: "chat")
   */
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
    // Verifica o campo "fromMe" do payload
    Object fromMeObj = data.get("fromMe");

    if (fromMeObj instanceof Boolean) {
      return (Boolean) fromMeObj;
    }

    if (fromMeObj instanceof String) {
      String fromMeStr = (String) fromMeObj;
      return "true".equalsIgnoreCase(fromMeStr) || "1".equals(fromMeStr);
    }

    // Verifica também o campo alternativo "from_me" (alguns webhooks usam snake_case)
    Object fromMe2Obj = data.get("from_me");
    if (fromMe2Obj instanceof Boolean) {
      return (Boolean) fromMe2Obj;
    }

    if (fromMe2Obj instanceof String) {
      String fromMe2Str = (String) fromMe2Obj;
      return "true".equalsIgnoreCase(fromMe2Str) || "1".equals(fromMe2Str);
    }

    // Por padrão, assume que é mensagem do cliente
    return false;
  }
}
