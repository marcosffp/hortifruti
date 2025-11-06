package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.model.chatbot.ChatSession;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionContext;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionStatus;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import com.hortifruti.sl.hortifruti.service.chatbot.ChatSessionService;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
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
    private final InvoiceService invoiceService;

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
                "*1* - Boleto - Consultar boletos em aberto\n" +
                "*2* - Pedido - Dúvidas sobre pedidos\n" +
                "*3* - Outro assunto - Falar com atendimento\n\n" +
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
     * Busca e envia boletos e notas fiscais pendentes de um cliente específico.
     * 
     * Localiza o cliente pelo documento (CPF/CNPJ), busca:
     * 1. Todos os combined scores pendentes (para boletos)
     * 2. Todas as notas fiscais autorizadas pela API Focus NFe (usando CPF/CNPJ)
     * 
     * Envia:
     * - Boletos (se houver hasBillet = true)
     * - Notas Fiscais/DANFE (buscadas pela API usando CPF/CNPJ)
     * - Apenas mensagem informativa se não houver arquivos
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
            log.info("========================================");
            log.info("Cliente encontrado: {} (ID: {})", client.getClientName(), client.getId());
            log.info("Documento: {}", document);

            // Busca Combined Scores pendentes COM BOLETO (hasBillet = true)
            List<CombinedScore> pendingWithBillet = billetService.findAllPendingWithBilletByClient(client.getId());
            log.info("Combined Scores pendentes COM BOLETO: {}", pendingWithBillet.size());
            
            // Busca TODOS os Combined Scores pendentes (para informar ao cliente)
            List<CombinedScore> allPending = billetService.findAllPendingByClient(client.getId());
            log.info("Combined Scores pendentes TOTAL: {}", allPending.size());

            // Busca todas as notas fiscais pela API Focus NFe usando CPF/CNPJ
            log.info("Iniciando busca de notas fiscais na API Focus NFe...");
            List<String> invoiceRefs = new ArrayList<>();
            try {
                invoiceRefs = invoiceService.listInvoiceRefsByDocument(document);
                log.info("✓ Notas fiscais autorizadas encontradas: {}", invoiceRefs.size());
                if (!invoiceRefs.isEmpty()) {
                    log.info("Refs encontradas: {}", String.join(", ", invoiceRefs));
                }
            } catch (Exception ex) {
                log.error("✗ Erro ao buscar notas fiscais na API: {}", ex.getMessage(), ex);
            }
            log.info("========================================");

            // Se não houver cobranças pendentes e nem notas fiscais
            if (allPending.isEmpty() && invoiceRefs.isEmpty()) {
                String message = String.format("Olá, %s!\n\n" +
                        "Boa notícia! Você não possui cobranças pendentes nem notas fiscais no momento.\n\n" +
                        "Se tiver alguma dúvida, entre em contato conosco:\n" +
                        "(31) 3641-2244", client.getClientName());
                whatsAppService.sendTextMessage(phoneNumber, message);
                return;
            }

            // Monta mensagem resumo
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(String.format("Olá, %s!\n\n", client.getClientName()));

            // Contadores
            int totalWithBillet = pendingWithBillet.size();
            int totalWithoutBillet = allPending.size() - pendingWithBillet.size();
            int totalInvoices = invoiceRefs.size();

            // Informações sobre cobranças pendentes
            if (!allPending.isEmpty()) {
                messageBuilder.append(String.format("📋 *Cobranças Pendentes:* %d\n\n", allPending.size()));

                int i = 1;
                for (CombinedScore cs : allPending) {
                    messageBuilder.append(String.format("*Cobrança %d:*\n", i));
                    messageBuilder.append(String.format("Valor: R$ %.2f\n", cs.getTotalValue()));
                    messageBuilder.append(String.format("Vencimento: %s\n", 
                        cs.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
                    
                    if (cs.isHasBillet()) {
                        messageBuilder.append(String.format("✓ Boleto: %s\n", 
                            cs.getYourNumber() != null ? cs.getYourNumber() : "Disponível"));
                    } else {
                        messageBuilder.append("○ Boleto: Não emitido ainda\n");
                    }
                    
                    if (i < allPending.size()) {
                        messageBuilder.append("────────────────\n\n");
                    }
                    i++;
                }
            }

            // Informações sobre notas fiscais
            if (totalInvoices > 0) {
                if (!allPending.isEmpty()) {
                    messageBuilder.append("\n────────────────\n\n");
                }
                messageBuilder.append(String.format("📄 *Notas Fiscais Autorizadas:* %d\n", totalInvoices));
            }
            
            // Resumo de documentos disponíveis
            messageBuilder.append("\n────────────────\n\n");
            messageBuilder.append("📦 *Documentos Disponíveis:*\n");
            if (totalWithBillet > 0) {
                messageBuilder.append(String.format("✓ %d Boleto(s)\n", totalWithBillet));
            }
            if (totalInvoices > 0) {
                messageBuilder.append(String.format("✓ %d Nota(s) Fiscal(is)\n", totalInvoices));
            }
            if (totalWithBillet == 0 && totalInvoices == 0) {
                messageBuilder.append("⚠️ Nenhum documento disponível no momento\n");
            }
            
            log.info("Resumo - Cobranças: {}, Com boleto: {}, Sem boleto: {}, Notas Fiscais: {}", 
                allPending.size(), totalWithBillet, totalWithoutBillet, totalInvoices);
            
            whatsAppService.sendTextMessage(phoneNumber, messageBuilder.toString());

            // Listas para armazenar os documentos (boletos e notas fiscais)
            List<byte[]> documents = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();
            
            log.info("========================================");
            log.info("Iniciando coleta de documentos...");
            log.info("Combined Scores COM BOLETO a processar: {}", pendingWithBillet.size());
            log.info("Notas Fiscais a processar: {}", invoiceRefs.size());
            
            // DEBUG: Listar todos os Combined Scores com boleto
            for (int idx = 0; idx < pendingWithBillet.size(); idx++) {
                CombinedScore cs = pendingWithBillet.get(idx);
                log.info("  [{}] ID: {}, YourNumber: {}, HasBillet: {}", 
                    idx + 1, cs.getId(), cs.getYourNumber(), cs.isHasBillet());
            }
            
            // 1. Processar APENAS boletos dos Combined Scores que têm hasBillet = true
            log.info("Processando {} boletos...", pendingWithBillet.size());
            int boletosAdicionados = 0;
            for (int idx = 0; idx < pendingWithBillet.size(); idx++) {
                CombinedScore cs = pendingWithBillet.get(idx);
                try {
                    log.info("  → [{}/{}] Obtendo boleto para CombinedScore ID: {} (YourNumber: {})", 
                        idx + 1, pendingWithBillet.size(), cs.getId(), cs.getYourNumber());
                    ResponseEntity<byte[]> pdfResponse = billetService.issueCopy(cs.getId());
                    byte[] pdf = pdfResponse.getBody();
                    
                    if (pdf != null && pdf.length > 0) {
                        String fileName = "Boleto-" + 
                            (cs.getYourNumber() != null && !cs.getYourNumber().isEmpty() 
                                ? cs.getYourNumber() 
                                : cs.getId()) + ".pdf";
                        documents.add(pdf);
                        fileNames.add(fileName);
                        boletosAdicionados++;
                        log.info("    ✓ Boleto adicionado: {} ({} bytes) - Total: {}/{}", 
                            fileName, pdf.length, boletosAdicionados, pendingWithBillet.size());
                    } else {
                        log.warn("    ✗ Boleto retornado é nulo ou vazio para ID: {}", cs.getId());
                    }
                } catch (Exception ex) {
                    log.error("    ✗ Falha ao gerar PDF do boleto para ID {}: {}", 
                        cs.getId(), ex.getMessage(), ex);
                }
            }
            log.info("Total de boletos adicionados: {}/{}", boletosAdicionados, pendingWithBillet.size());
            log.info("Total de boletos adicionados: {}/{}", boletosAdicionados, pendingWithBillet.size());
            
            // 2. Processar TODAS as notas fiscais buscadas pela API Focus NFe
            log.info("Processando {} notas fiscais...", invoiceRefs.size());
            int notasAdicionadas = 0;
            for (int idx = 0; idx < invoiceRefs.size(); idx++) {
                String ref = invoiceRefs.get(idx);
                try {
                    log.info("  → [{}/{}] Obtendo DANFE para invoiceRef: {}", 
                        idx + 1, invoiceRefs.size(), ref);
                    ResponseEntity<Resource> danfeResponse = invoiceService.downloadDanfe(ref);
                    Resource resource = danfeResponse.getBody();
                    
                    if (resource != null) {
                        byte[] danfePdf = resource.getContentAsByteArray();
                        if (danfePdf != null && danfePdf.length > 0) {
                            String fileName = "NotaFiscal-" + ref + ".pdf";
                            documents.add(danfePdf);
                            fileNames.add(fileName);
                            notasAdicionadas++;
                            log.info("    ✓ Nota Fiscal adicionada: {} ({} bytes) - Total: {}/{}", 
                                fileName, danfePdf.length, notasAdicionadas, invoiceRefs.size());
                        } else {
                            log.warn("    ✗ DANFE retornado é nulo ou vazio para ref: {}", ref);
                        }
                    } else {
                        log.warn("    ✗ Resource DANFE é nulo para ref: {}", ref);
                    }
                } catch (Exception ex) {
                    log.error("    ✗ Falha ao obter DANFE para ref {}: {}", 
                        ref, ex.getMessage(), ex);
                }
            }
            log.info("Total de notas fiscais adicionadas: {}/{}", notasAdicionadas, invoiceRefs.size());
            log.info("Total de notas fiscais adicionadas: {}/{}", notasAdicionadas, invoiceRefs.size());
            
            log.info("Coleta finalizada:");
            log.info("  • Boletos: {}/{}", boletosAdicionados, pendingWithBillet.size());
            log.info("  • Notas Fiscais: {}/{}", notasAdicionadas, invoiceRefs.size());
            log.info("  • Total de documentos coletados: {}", documents.size());
            log.info("Lista de arquivos coletados:");
            for (int i = 0; i < fileNames.size(); i++) {
                log.info("  [{}] {} ({} bytes)", i + 1, fileNames.get(i), documents.get(i).length);
            }
            log.info("========================================");
            
            // 3. Enviar documentos se houver algum
            if (!documents.isEmpty()) {
                int totalDocs = documents.size();
                
                // Detalhar quais documentos serão enviados
                int boletosCount = 0;
                int notasCount = 0;
                for (String name : fileNames) {
                    if (name.startsWith("Boleto-")) boletosCount++;
                    if (name.startsWith("NotaFiscal-")) notasCount++;
                }
                
                String caption = String.format("📎 Enviando %d documento(s):\n", totalDocs);
                if (boletosCount > 0) {
                    caption += String.format("• %d Boleto(s)\n", boletosCount);
                }
                if (notasCount > 0) {
                    caption += String.format("• %d Nota(s) Fiscal(is)\n", notasCount);
                }
                
                log.info("========================================");
                log.info("PREPARANDO ENVIO DE DOCUMENTOS");
                log.info("Destinatário: {}", phoneNumber);
                log.info("Total de documentos a enviar: {}", totalDocs);
                log.info("  • Boletos: {}", boletosCount);
                log.info("  • Notas Fiscais: {}", notasCount);
                log.info("Documentos na lista:");
                for (int i = 0; i < fileNames.size(); i++) {
                    log.info("  [{}] {} ({} bytes)", i + 1, fileNames.get(i), documents.get(i).length);
                }
                log.info("========================================");
                
                boolean sent = whatsAppService.sendMultipleDocuments(phoneNumber, caption, documents, fileNames);
                
                if (sent) {
                    log.info("✓ SUCESSO: Todos os {} documentos foram enviados com sucesso!", totalDocs);
                } else {
                    log.error("✗ FALHA: Um ou mais documentos não foram enviados corretamente");
                }
            } else {
                // Se não houver documentos disponíveis para envio
                log.warn("Nenhum documento disponível para envio (Cobranças: {}, Boletos emitidos: {}, Notas: {})", 
                    allPending.size(), pendingWithBillet.size(), invoiceRefs.size());
                
                String noDocumentsMessage = "⚠️ *Documentos Pendentes*\n\n";
                
                if (totalWithoutBillet > 0) {
                    noDocumentsMessage += String.format("Você possui %d cobrança(s) sem boleto emitido ainda.\n", 
                        totalWithoutBillet);
                }
                
                if (invoiceRefs.isEmpty() && !allPending.isEmpty()) {
                    noDocumentsMessage += "As notas fiscais estão sendo processadas ou ainda não foram emitidas.\n";
                }
                
                noDocumentsMessage += "\n*Entre em contato para mais informações:*\n" +
                        "📞 (31) 3641-2244\n\n" +
                        "Horário de atendimento:\n" +
                        "• Segunda a Sábado, 7h às 20h\n" +
                        "• Domingo, 7h às 12h";
                
                whatsAppService.sendTextMessage(phoneNumber, noDocumentsMessage);
            }

            // Associar cliente à sessão
            chatSessionService.associateClient(session.getId(), client.getId());
            
            // Deleta a sessão após enviar os documentos (limpa o banco)
            chatSessionService.closeSession(session.getId(), "COMPLETED");
            log.info("Sessão {} finalizada para cliente {}", session.getId(), client.getId());

        } catch (Exception e) {
            log.error("Erro ao processar solicitação de documentos para {}: {}", phoneNumber, e.getMessage(), e);
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