package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.model.chatbot.ChatSession;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionContext;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionStatus;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import com.hortifruti.sl.hortifruti.service.chatbot.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço responsável pelo processamento de mensagens do chatbot WhatsApp.
 * 
 * Gerencia a interação com clientes através do WhatsApp, processando comandos
 * relacionados a consulta de boletos, solicitações de ajuda e saudações.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final WhatsAppService whatsAppService;
    private final BilletService billetService;
    private final ClientRepository clientRepository;
    private final ChatSessionService chatSessionService;

        /**
     * Processa mensagens recebidas do webhook do WhatsApp.
     * 
     * Extrai informações do payload, valida se é uma mensagem privada válida
     * e encaminha para processamento de comandos.
     * 
     * Detecta automaticamente mensagens manuais enviadas por atendentes
     * e pausa o bot por 1 hora para evitar conflitos.
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
            
            if (!from.endsWith("@c.us")) {
                return;
            }
            
            String phoneNumber = extractPhoneFromJid(from);
            String messageBody = extractMessageBodyUltraMsg(data);
            String messageType = extractMessageTypeUltraMsg(data);

            if (!"chat".equals(messageType)) {
                return;
            }

            // Detecta se a mensagem foi enviada manualmente (não é do cliente)
            // Mensagens do bot têm o campo "fromMe" = true no payload do UltraMsg
            boolean isFromMe = detectIfMessageIsFromBot(data);
            
            if (isFromMe) {
                // Mensagem enviada manualmente pelo atendente via WhatsApp
                log.info("Mensagem manual detectada para {}. Pausando bot por 1 hora e mudando status para PAUSED.", phoneNumber);
                chatSessionService.pauseBotForPhone(phoneNumber, 1);
                
                // Muda o status da sessão para PAUSED (atendimento humano em andamento)
                ChatSession session = chatSessionService.getOrCreateSession(phoneNumber);
                chatSessionService.updateSessionStatus(session.getId(), SessionStatus.PAUSED);
                
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
     * Gerencia o fluxo completo da conversa através de sessões, incluindo:
     * - Menu de opções
     * - Consulta de boletos por CPF/CNPJ
     * - Encaminhamento para atendimento humano
     * 
     * @param phoneNumber Número de telefone do remetente
     * @param message Conteúdo da mensagem enviada
     */
    private void processCommand(String phoneNumber, String message) {
        try {
            // 1. Verificar se o bot está pausado para este número
            if (chatSessionService.isBotPausedForPhone(phoneNumber)) {
                log.info("Bot pausado para telefone {}. Mensagem ignorada.", phoneNumber);
                return;
            }

            // 2. Verificar comandos globais (funcionam em qualquer estado)
            String normalized = message.toLowerCase().trim();
            if (normalized.equals("menu") || normalized.equals("recomeçar") || 
                normalized.equals("recomecar")) {
                // Reseta a sessão e volta ao menu principal
                ChatSession session = chatSessionService.getOrCreateSession(phoneNumber);
                chatSessionService.updateSessionStatus(session.getId(), SessionStatus.MENU);
                chatSessionService.setSessionContext(session.getId(), null); // Limpa contexto
                sendMainMenu(phoneNumber);
                log.info("Cliente {} solicitou voltar ao menu principal", phoneNumber);
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
                    // Não responde para não atrapalhar o atendente
                    log.info("Bot pausado para {}. Atendimento humano em andamento.", phoneNumber);
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

    /**
     * Processa a seleção do menu principal
     */
    private void handleMenuSelection(ChatSession session, String phoneNumber, String message) {
        String normalized = message.toLowerCase().trim();
        
        // Opção 1: Boleto
        if (normalized.equals("1") || normalized.contains("boleto")) {
            chatSessionService.setSessionContext(session.getId(), SessionContext.BOLETO);
            chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_DOCUMENT);
            String msg = "Para consultar seus boletos, por favor, envie seu CPF *(apenas números)* ou CNPJ.\n\n" +
                    " Digite MENU para voltar ao início";
            whatsAppService.sendTextMessage(phoneNumber, msg);
            return;
        }
        
        // Opção 2: Pedido
        if (normalized.equals("2") || normalized.contains("pedido")) {
            chatSessionService.setSessionContext(session.getId(), SessionContext.PEDIDO);
            chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_HUMAN);
            String msg = "📋 *Fazer Pedido*\n\n" +
                    "Por favor, envie a lista de produtos que deseja:\n" +
                    "Nossa equipe vai receber seu pedido e responder em breve com disponibilidade e valores.\n\n" +
                    "Horário de atendimento: \n"+
                    "• Segunda a Sábado, 7h às 20h.\n"+
                    "• Domingo, das 7h às 12h";
            whatsAppService.sendTextMessage(phoneNumber, msg);
            return;
        }
        
        // Opção 3: Outro assunto
        if (normalized.equals("3") || normalized.contains("outro")) {
            chatSessionService.setSessionContext(session.getId(), SessionContext.OUTRO);
            chatSessionService.updateSessionStatus(session.getId(), SessionStatus.AWAITING_HUMAN);
            String msg = "💬 *Falar com Atendimento*\n\n" +
                    "Por favor, descreva seu assunto ou dúvida:\n" +
                    "Nossa equipe vai receber sua mensagem e responder em breve.\n\n" +
                   "Horário de atendimento: \n"+
                    "• Segunda a Sábado, 7h às 20h.\n"+
                    "• Domingo, das 7h às 12h";
            whatsAppService.sendTextMessage(phoneNumber, msg);
            return;
        }
        
        // Opção não reconhecida, reenvia menu
        sendMainMenu(phoneNumber);
    }

    /**
     * Envia o menu principal
     */
    private void sendMainMenu(String phoneNumber) {
        String menu = "Olá! Bem-vindo ao Hortifruti SL!\n\n" +
                "Como posso te ajudar hoje? Digite o número da opção:\n\n" +
                "*1* Boleto - Consultar boletos em aberto\n" +
                "*2* Pedido - Dúvidas sobre pedidos\n" +
                "*3* Outro assunto - Falar com atendimento\n\n" +
                "Digite o número da opção desejada (1, 2 ou 3)\n\n" +
                "A qualquer momento, digite MENU para voltar aqui";
        whatsAppService.sendTextMessage(phoneNumber, menu);
    }

    /**
     * Processa entrada de documento (CPF/CNPJ)
     */
    private void handleDocumentInput(ChatSession session, String phoneNumber, String message) {
        String onlyDigits = message.replaceAll("[^0-9]", "");
        
        if (onlyDigits.length() == 11 || onlyDigits.length() == 14) {
            handleBilletRequestByDocument(session, phoneNumber, onlyDigits);
        } else {
            String msg = "Documento inválido. Por favor, envie um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.\n\n" +
                    "Exemplo: 12345678900 ou 12345678000190";
            whatsAppService.sendTextMessage(phoneNumber, msg);
        }
    }

    /**
     * Busca e envia boletos pendentes de um cliente específico.
     * 
     * Localiza o cliente pelo documento (CPF/CNPJ), busca todos os boletos
     * pendentes com boleto emitido e envia uma mensagem com o resumo seguida
     * dos PDFs dos boletos.
     * 
     * @param session Sessão de chat ativa
     * @param phoneNumber Número de telefone do cliente
     * @param document CPF ou CNPJ do cliente (apenas dígitos)
     */
    private void handleBilletRequestByDocument(ChatSession session, String phoneNumber, String document) {
        try {
            Optional<Client> clientOpt = clientRepository.findByDocument(document);
            if (clientOpt.isEmpty()) {
                String message = "Desculpe, não encontrei nenhum cliente com esse documento em nosso sistema.\n\n" +
                        "Verifique se o CPF ou CNPJ está correto ou entre em contato conosco:\n" +
                        "(31) 3641-2244";
                whatsAppService.sendTextMessage(phoneNumber, message);
                return;
            }

            Client client = clientOpt.get();

            List<CombinedScore> clientOverdue = billetService.findAllPendingWithBilletByClient(client.getId());

            if (clientOverdue.isEmpty()) {
                String message = String.format("Olá, %s!\n\n" +
                        "Boa notícia! Você não possui boletos vencidos e pendentes no momento.\n\n" +
                        "Se tiver alguma dúvida, entre em contato conosco:\n" +
                        "(31) 3641-2244", client.getClientName());
                whatsAppService.sendTextMessage(phoneNumber, message);
                return;
            }

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(String.format("Olá, %s!\n\n", client.getClientName()));
            messageBuilder.append(String.format("Você possui %d boleto(s) vencido(s) e pendente(s):\n\n", clientOverdue.size()));

            int i = 1;
            for (CombinedScore cs : clientOverdue) {
                messageBuilder.append(String.format("Boleto %d:\n", i));
                messageBuilder.append(String.format("Valor: R$ %.2f\n", cs.getTotalValue()));
                messageBuilder.append(String.format("Vencimento: %s\n", 
                    cs.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
                messageBuilder.append(String.format("Número: %s\n", 
                    cs.getYourNumber() != null ? cs.getYourNumber() : "-"));
                
                if (i < clientOverdue.size()) {
                    messageBuilder.append("────────────────\n\n");
                }
                i++;
            }
            
            whatsAppService.sendTextMessage(phoneNumber, messageBuilder.toString());

            List<byte[]> pdfs = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();
            
            for (CombinedScore cs : clientOverdue) {
                try {
                    ResponseEntity<byte[]> pdfResponse = billetService.issueCopy(cs.getId());
                    byte[] pdf = pdfResponse.getBody();
                    
                    if (pdf != null && pdf.length > 0) {
                        String fileName = "Boleto-" + 
                            (cs.getYourNumber() != null && !cs.getYourNumber().isEmpty() 
                                ? cs.getYourNumber() 
                                : cs.getId()) + ".pdf";
                        pdfs.add(pdf);
                        fileNames.add(fileName);
                    }
                } catch (Exception ex) {
                    log.warn("Falha ao gerar PDF do boleto {}: {}", cs.getId(), ex.getMessage());
                }
            }
            
            if (!pdfs.isEmpty()) {
                whatsAppService.sendMultipleDocuments(phoneNumber, "Segue seus boletos em aberto.", pdfs, fileNames);
            }

            // Associar cliente à sessão
            chatSessionService.associateClient(session.getId(), client.getId());
            
            // Deleta a sessão após enviar os boletos (limpa o banco)
            chatSessionService.closeSession(session.getId(), "COMPLETED");

        } catch (Exception e) {
            log.error("Erro ao processar solicitação de boletos para {}: {}", phoneNumber, e.getMessage(), e);
            sendErrorMessage(phoneNumber);
        }
    }

    /**
     * Envia mensagem informando que o comando não foi reconhecido
     * e lista os comandos disponíveis.
     * 
     * @param phoneNumber Número de telefone do destinatário
     */
    private void handleUnknownCommand(String phoneNumber) {
        String message = "Desculpe, não entendi sua solicitação.\n\n" +
                "Comandos disponíveis:\n" +
                "- 'boletos' - Ver cobranças em aberto\n" +
                "- 'ajuda' - Lista de comandos\n" +
                "- 'oi' - Saudação e boas-vindas\n\n" +
                "Tente usar uma dessas palavras-chave!\n\n" +
                "Para outras dúvidas: (31) 3641-2244";
        
        whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Envia mensagem genérica de erro ao cliente.
     * 
     * @param phoneNumber Número de telefone do destinatário
     */
    private void sendErrorMessage(String phoneNumber) {
        String message = "Ops! Ocorreu um erro temporário.\n\n" +
                "Por favor, tente novamente em alguns minutos ou entre em contato:\n\n" +
                "(31) 3641-2244\n" +
                "Segunda a Sexta, 8h às 18h";
        
        whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Extrai o número de telefone do JID do WhatsApp.
     * 
     * Remove o sufixo @c.us do identificador retornando apenas os dígitos.
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
     * No payload do UltraMsg, mensagens enviadas pelo número conectado
     * (bot ou atendente manual) têm o campo "fromMe" = true.
     * Mensagens recebidas de clientes têm "fromMe" = false ou ausente.
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