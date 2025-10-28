package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import com.hortifruti.sl.hortifruti.service.notification.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final WhatsAppService whatsAppService;
    private final BilletService billetService;
    private final ClientRepository clientRepository;

    /**
     * Processa mensagens recebidas via webhook do WhatsApp
     */
    public void processIncomingMessage(Map<String, Object> payload) {
        try {
            // Extrair informações da mensagem
            Object dataObj = payload.get("data");
            if (!(dataObj instanceof Map)) {
                log.warn("Payload sem campo 'data' válido");
                return;
            }
            Map<String, Object> data = (Map<String, Object>) dataObj;
            String from = (String) data.getOrDefault("from", "");
            // Ignorar mensagens de grupo
            if (!from.endsWith("@c.us")) {
                log.info("Mensagem ignorada (não é privada): from={}", from);
                return;
            }
            String phoneNumber = extractPhoneFromJid(from);
            String messageBody = extractMessageBodyUltraMsg(data);
            String messageType = extractMessageTypeUltraMsg(data);

            log.info("Processando mensagem - Telefone: {}, Tipo: {}, Mensagem: {}", 
                    phoneNumber, messageType, messageBody);

            // Verificar se é uma mensagem de texto (não mídia, áudio, etc.)
            if (!"chat".equals(messageType)) {
                log.info("Tipo de mensagem não suportado: {}", messageType);
                return;
            }

            // Processar comandos do chatbot
            processCommand(phoneNumber, messageBody);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem recebida: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa comandos do chatbot baseado na mensagem recebida
     */
    private void processCommand(String phoneNumber, String message) {
        String normalizedMessage = message.toLowerCase().trim();

        try {
            // Se o cliente enviar um documento (CPF ou CNPJ)
            String onlyDigits = message.replaceAll("[^0-9]", "");
            if (onlyDigits.length() == 11 || onlyDigits.length() == 14) {
                handleBilletRequestByDocument(phoneNumber, onlyDigits);
                return;
            }

            // Comando para receber boletos
            if (normalizedMessage.contains("boleto") || 
                normalizedMessage.contains("cobrança") ||
                normalizedMessage.contains("cobranca") ||
                normalizedMessage.contains("fatura") ||
                normalizedMessage.contains("conta") ||
                normalizedMessage.contains("pagamento")) {
                // Solicitar o documento do cliente
                String msg = "Para consultar seus boletos, por favor, envie seu CPF (apenas números) ou CNPJ.";
                whatsAppService.sendTextMessage(phoneNumber, msg);
                return;
            }

            // Comando de ajuda
            if (normalizedMessage.contains("ajuda") || 
                normalizedMessage.contains("help") ||
                normalizedMessage.contains("menu") ||
                normalizedMessage.contains("comandos")) {
                handleHelpRequest(phoneNumber);
                return;
            }

            // Saudações
            if (normalizedMessage.contains("oi") || 
                normalizedMessage.contains("olá") ||
                normalizedMessage.contains("ola") ||
                normalizedMessage.contains("bom dia") ||
                normalizedMessage.contains("boa tarde") ||
                normalizedMessage.contains("boa noite")) {
                handleGreeting(phoneNumber);
                return;
            }

            // Mensagem padrão para comandos não reconhecidos
            handleUnknownCommand(phoneNumber);

        } catch (Exception e) {
            log.error("Erro ao processar comando para {}: {}", phoneNumber, e.getMessage(), e);
            sendErrorMessage(phoneNumber);
        }
    }

    /**
     * Processa solicitação de boletos
     */
    // Novo método: busca boletos pelo documento informado
    private void handleBilletRequestByDocument(String phoneNumber, String document) {
        try {
            // Buscar cliente pelo documento
            Optional<Client> clientOpt = clientRepository.findByDocument(document);
            if (clientOpt.isEmpty()) {
                String message = "Desculpe, não encontrei nenhum cliente com esse documento em nosso sistema.\n\n" +
                        "Verifique se o CPF ou CNPJ está correto ou entre em contato conosco:\n" +
                        "(31) 3641-2244";
                whatsAppService.sendTextMessage(phoneNumber, message);
                return;
            }

            Client client = clientOpt.get();
            log.info("Cliente encontrado por documento: {} (ID: {})", client.getClientName(), client.getId());

            // Buscar boletos vencidos e pendentes desse cliente
            List<com.hortifruti.sl.hortifruti.model.purchase.CombinedScore> allOverdue = billetService.syncAndFindOverdueUnpaidScores(java.time.LocalDate.now());
            List<com.hortifruti.sl.hortifruti.model.purchase.CombinedScore> clientOverdue = allOverdue.stream()
                .filter(cs -> cs.getClientId() == client.getId())
                .toList();

            if (clientOverdue.isEmpty()) {
                String message = String.format("Olá, %s!\n\n" +
                        "Boa notícia! Você não possui boletos vencidos e pendentes no momento.\n\n" +
                        "Se tiver alguma dúvida, entre em contato conosco:\n" +
                        "(31) 3641-2244", client.getClientName());
                whatsAppService.sendTextMessage(phoneNumber, message);
                return;
            }

            // Montar mensagem com os boletos vencidos
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(String.format("Olá, %s!\n\n", client.getClientName()));
            messageBuilder.append(String.format("Você possui %d boleto(s) vencido(s) e pendente(s):\n\n", clientOverdue.size()));

            int i = 1;
            for (com.hortifruti.sl.hortifruti.model.purchase.CombinedScore cs : clientOverdue) {
                messageBuilder.append(String.format("Boleto %d:\n", i++));
                messageBuilder.append(String.format("Valor: R$ %.2f\n", cs.getTotalValue()));
                messageBuilder.append(String.format("Vencimento: %s\n", formatDate(cs.getDueDate().toString())));
                messageBuilder.append(String.format("Número: %s\n", cs.getYourNumber() != null ? cs.getYourNumber() : "-"));
                if (i <= clientOverdue.size()) {
                    messageBuilder.append("────────────────\n\n");
                }
            }

            messageBuilder.append("\n\nComo pagar:\n");
            messageBuilder.append("- Acesse nosso site para gerar a segunda via\n");
            messageBuilder.append("- Use o código de barras no seu banco\n");
            messageBuilder.append("- Pague no PIX usando o QR Code\n\n");
            messageBuilder.append("Precisa de ajuda? Entre em contato:\n");
            messageBuilder.append("(31) 3641-2244");

            whatsAppService.sendTextMessage(phoneNumber, messageBuilder.toString());

        } catch (Exception e) {
            log.error("Erro ao processar solicitação de boletos por documento para {}: {}", phoneNumber, e.getMessage(), e);
            sendErrorMessage(phoneNumber);
        }
    }

    /**
     * Processa solicitação de ajuda
     */
    private void handleHelpRequest(String phoneNumber) {
    String message = "Hortifruti SL - Assistente Virtual\n\n" +
            "Olá! Eu posso te ajudar com:\n\n" +
            "Boletos e Cobranças\n" +
            "- Digite: 'boletos', 'cobrança' ou 'fatura'\n" +
            "- Vou mostrar seus boletos em aberto\n\n" +
            "Contato Direto\n" +
            "- Telefone: (31) 3641-2244\n" +
            "- Horário: Segunda a Sexta, 8h às 18h\n\n" +
            "Dicas:\n" +
            "- Use palavras simples\n" +
            "- Uma solicitação por vez\n\n" +
            "Como posso te ajudar hoje?";
        
        whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Processa saudações
     */
    private void handleGreeting(String phoneNumber) {
    String message = "Olá! Bem-vindo ao Hortifruti SL!\n\n" +
            "Sou seu assistente virtual e estou aqui para ajudar!\n\n" +
            "O que posso fazer por você:\n" +
            "- Consultar seus boletos em aberto\n" +
            "- Fornecer informações de contato\n" +
            "- Tirar dúvidas básicas\n\n" +
            "Digite 'boletos' para ver suas cobranças\n" +
            "Digite 'ajuda' para ver todos os comandos\n\n" +
            "Como posso te ajudar?";
        
        whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Processa comandos não reconhecidos
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
     * Envia mensagem de erro genérica
     */
    private void sendErrorMessage(String phoneNumber) {
    String message = "Ops! Ocorreu um erro temporário.\n\n" +
            "Por favor, tente novamente em alguns minutos ou entre em contato:\n\n" +
            "(31) 3641-2244\n" +
            "Segunda a Sexta, 8h às 18h";
        
        whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Busca cliente pelo número de telefone
    //  */
    // private Optional<Client> findClientByPhone(String phoneNumber) {
    //     try {
    //         // Limpar e formatar número para busca
    //         String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
            
    //         // Remover código do país se presente
    //         if (cleanPhone.startsWith("55") && cleanPhone.length() > 11) {
    //             cleanPhone = cleanPhone.substring(2);
    //         }
            
    //         // Tentar diferentes variações do número
    //         List<String> phoneVariations = List.of(
    //             cleanPhone,
    //             cleanPhone.length() == 10 ? cleanPhone.substring(0, 2) + "9" + cleanPhone.substring(2) : cleanPhone,
    //             cleanPhone.length() == 11 && cleanPhone.charAt(2) == '9' ? 
    //                 cleanPhone.substring(0, 2) + cleanPhone.substring(3) : cleanPhone,
    //             cleanPhone.length() >= 8 ? cleanPhone.substring(cleanPhone.length() - 8) : cleanPhone,
    //             cleanPhone.length() >= 9 ? cleanPhone.substring(cleanPhone.length() - 9) : cleanPhone
    //         );

    //         for (String variation : phoneVariations) {
    //             // Tentar busca com números limpos primeiro
    //             List<Client> clients = clientRepository.findByCleanPhoneNumberContaining(variation);
    //             if (!clients.isEmpty()) {
    //                 log.info("Cliente encontrado com variação: {} para número original: {}", variation, phoneNumber);
    //                 return Optional.of(clients.get(0));
    //             }
                
    //             // Tentar busca simples também
    //             clients = clientRepository.findByPhoneNumberContaining(variation);
    //             if (!clients.isEmpty()) {
    //                 log.info("Cliente encontrado com busca simples: {} para número original: {}", variation, phoneNumber);
    //                 return Optional.of(clients.get(0));
    //             }
    //         }

    //         log.warn("Nenhum cliente encontrado para o número: {} (limpo: {})", phoneNumber, cleanPhone);
    //         return Optional.empty();
            
    //     } catch (Exception e) {
    //         log.error("Erro ao buscar cliente por telefone {}: {}", phoneNumber, e.getMessage(), e);
    //         return Optional.empty();
    //     }
    // }

    // ...existing code...

    /**
     * Extrai apenas o número do JID do WhatsApp (ex: 559999999999@c.us)
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
     * Extrai corpo da mensagem do payload UltraMsg (campo 'body' dentro de 'data')
     */
    private String extractMessageBodyUltraMsg(Map<String, Object> data) {
        return (String) data.getOrDefault("body", "");
    }

    /**
     * Extrai tipo da mensagem do payload UltraMsg (campo 'type' dentro de 'data')
     */
    private String extractMessageTypeUltraMsg(Map<String, Object> data) {
        return (String) data.getOrDefault("type", "chat");
    }

    /**
     * Formata data para exibição
     */
    private String formatDate(String date) {
        try {
            // Assumindo que a data vem no formato YYYY-MM-DD ou similar
            // Você pode ajustar conforme o formato real da API
            return date;
        } catch (Exception e) {
            log.warn("Erro ao formatar data: {}", date);
            return date;
        }
    }
}