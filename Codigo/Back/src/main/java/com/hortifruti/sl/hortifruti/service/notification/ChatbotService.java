package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import com.hortifruti.sl.hortifruti.service.notification.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    /**
     * Processa mensagens recebidas através do webhook do WhatsApp.
     * 
     * Extrai informações do payload, valida se é uma mensagem privada válida
     * e encaminha para processamento de comandos.
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

            processCommand(phoneNumber, messageBody);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem recebida: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa comandos do chatbot baseado na mensagem recebida.
     * 
     * Identifica o tipo de comando através de palavras-chave e encaminha
     * para o handler apropriado. Suporta:
     * - Consulta de boletos por CPF/CNPJ
     * - Solicitação de ajuda
     * - Saudações
     * 
     * @param phoneNumber Número de telefone do remetente
     * @param message Conteúdo da mensagem enviada
     */
    private void processCommand(String phoneNumber, String message) {
        String normalizedMessage = message.toLowerCase().trim();

        try {
            String onlyDigits = message.replaceAll("[^0-9]", "");
            if (onlyDigits.length() == 11 || onlyDigits.length() == 14) {
                handleBilletRequestByDocument(phoneNumber, onlyDigits);
                return;
            }

            if (normalizedMessage.contains("boleto") || 
                normalizedMessage.contains("cobrança") ||
                normalizedMessage.contains("cobranca") ||
                normalizedMessage.contains("fatura") ||
                normalizedMessage.contains("conta") ||
                normalizedMessage.contains("pagamento")) {
                String msg = "Para consultar seus boletos, por favor, envie seu CPF (apenas números) ou CNPJ.";
                whatsAppService.sendTextMessage(phoneNumber, msg);
                return;
            }

            if (normalizedMessage.contains("ajuda") || 
                normalizedMessage.contains("help") ||
                normalizedMessage.contains("menu") ||
                normalizedMessage.contains("comandos")) {
                handleHelpRequest(phoneNumber);
                return;
            }

            if (normalizedMessage.contains("oi") || 
                normalizedMessage.contains("olá") ||
                normalizedMessage.contains("ola") ||
                normalizedMessage.contains("bom dia") ||
                normalizedMessage.contains("boa tarde") ||
                normalizedMessage.contains("boa noite")) {
                handleGreeting(phoneNumber);
                return;
            }

            handleUnknownCommand(phoneNumber);

        } catch (Exception e) {
            log.error("Erro ao processar comando para {}: {}", phoneNumber, e.getMessage(), e);
            sendErrorMessage(phoneNumber);
        }
    }

    /**
     * Busca e envia boletos pendentes de um cliente específico.
     * 
     * Localiza o cliente pelo documento (CPF/CNPJ), busca todos os boletos
     * pendentes com boleto emitido e envia uma mensagem com o resumo seguida
     * dos PDFs dos boletos.
     * 
     * @param phoneNumber Número de telefone do cliente
     * @param document CPF ou CNPJ do cliente (apenas dígitos)
     */
    private void handleBilletRequestByDocument(String phoneNumber, String document) {
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

            messageBuilder.append("\n\nComo pagar:\n");
            messageBuilder.append("- Acesse nosso site para gerar a segunda via\n");
            messageBuilder.append("- Use o código de barras no seu banco\n");
            messageBuilder.append("- Pague no PIX usando o QR Code\n\n");
            messageBuilder.append("Precisa de ajuda? Entre em contato:\n");
            messageBuilder.append("(31) 3641-2244");

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

        } catch (Exception e) {
            log.error("Erro ao processar solicitação de boletos por documento para {}: {}", 
                phoneNumber, e.getMessage(), e);
            sendErrorMessage(phoneNumber);
        }
    }

    /**
     * Envia mensagem com lista de comandos disponíveis e instruções de uso.
     * 
     * @param phoneNumber Número de telefone do destinatário
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
     * Envia mensagem de boas-vindas com opções disponíveis.
     * 
     * @param phoneNumber Número de telefone do destinatário
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
}