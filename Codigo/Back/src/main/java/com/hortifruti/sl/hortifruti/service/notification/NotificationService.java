package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final EmailService emailService;
  private final WhatsAppService whatsAppService;
  private final FileGenerationService fileGenerationService;
  private final StatementSelectionService statementSelectionService;
  private final ClientRepository clientRepository;
  private final EmailTemplateService emailTemplateService;

  @Value("${accounting.email}")
  private String accountingEmail;

  @Value("${manager.email}")
  private String managerEmail;

  @Value("${accounting.whatsapp}")
  private String accountingWhatsapp;

  @Value("${manager.whatsapp}")
  private String managerWhatsapp;

  /**
   * Envio mensal para contabilidade: Extratos BB/Sicoob + Notas fiscais
   */
  @Transactional(readOnly = true)
  public NotificationResponse sendMonthlyStatements(MonthlyStatementsRequest request) {
    try {
      log.info("Enviando extratos mensais {}/{}", request.month(), request.year());
      
      // Gerar ZIP com statements (pega os com maior data do mês)
      byte[] zipFile = fileGenerationService.createZipWithStatements(request.month(), request.year());
      
      List<byte[]> attachments = List.of(zipFile);
      List<String> fileNames = List.of("extratos_" + request.month() + "_" + request.year() + ".zip");

      String subject = "Documentos Contábeis - " + request.month() + "/" + request.year();
      String message = buildMonthlyMessage(request);

      // Para extratos mensais, passar período e mensagem customizada se houver
      String period = request.month() + "/" + request.year();
      String customMessage = request.customMessage() != null ? request.customMessage() : "";
      return sendToAccounting(request.channel(), subject, message, attachments, fileNames, customMessage, period);
      
    } catch (Exception e) {
      log.error("Erro ao enviar extratos mensais", e);
      return new NotificationResponse(false, "Erro: " + e.getMessage());
    }
  }

  /**
   * Envio para contabilidade: Arquivos genéricos com cálculo de redução de 60%
   */
  public NotificationResponse sendGenericFilesToAccounting(List<MultipartFile> files, GenericFilesAccountingRequest request) {
    try {
      log.info("Enviando arquivos genéricos para contabilidade");
      
      List<byte[]> fileContents = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();
      
      // Converter arquivos
      for (MultipartFile file : files) {
        fileContents.add(file.getBytes());
        fileNames.add(file.getOriginalFilename());
      }
      
  // Somar todos os valores e aplicar desconto de 60%
  BigDecimal total = BigDecimal.ZERO;
  if (request.debitValue() != null) total = total.add(request.debitValue());
  if (request.creditValue() != null) total = total.add(request.creditValue());
  if (request.cashValue() != null) total = total.add(request.cashValue());
  BigDecimal totalComDesconto = total.multiply(BigDecimal.valueOf(0.4));

  String subject = "Arquivos Contábeis - Resumo Financeiro";
  String message = buildGenericFilesMessage(request, total, totalComDesconto);

      return sendToAccounting(request.channel(), subject, message, fileContents, fileNames, 
                             request.customMessage(), String.format("%.2f", totalComDesconto));
      
    } catch (IOException e) {
      log.error("Erro ao processar arquivos", e);
      return new NotificationResponse(false, "Erro ao processar arquivos: " + e.getMessage());
    }
  }

  /**
   * Envio para cliente: Boleto + Nota Fiscal + arquivos genéricos
   */
  public NotificationResponse sendDocumentsToClient(List<MultipartFile> files, ClientDocumentsRequest request) {
    try {
      log.info("Enviando documentos para cliente ID: {}", request.clientId());
      
      Optional<Client> clientOpt = clientRepository.findById(request.clientId());
      if (clientOpt.isEmpty()) {
        return new NotificationResponse(false, "Cliente não encontrado");
      }
      
      Client client = clientOpt.get();
      List<byte[]> attachments = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();
      
      // Adicionar arquivos enviados
      if (files != null && !files.isEmpty()) {
        for (MultipartFile file : files) {
          attachments.add(file.getBytes());
          fileNames.add(file.getOriginalFilename());
        }
      }

      String subject = "Documentos - " + client.getClientName();
      String message = buildClientMessage(request, client);

      return sendToClient(client, request.channel(), subject, message, attachments, fileNames, request.customMessage());
      
    } catch (IOException e) {
      log.error("Erro ao processar arquivos do cliente", e);
      return new NotificationResponse(false, "Erro ao processar arquivos: " + e.getMessage());
    }
  }



  /**
   * Teste dos serviços de comunicação
   */
  public NotificationResponse testCommunicationServices() {
    try {
      boolean emailOk = emailService != null && accountingEmail != null && !accountingEmail.isEmpty();
      boolean whatsappOk = whatsAppService != null && accountingWhatsapp != null && !accountingWhatsapp.isEmpty();
      
      String message = String.format("Email: %s, WhatsApp: %s", 
          emailOk ? "OK" : "FALHA", whatsappOk ? "OK" : "FALHA");
      
      return NotificationResponse.withStatuses(emailOk && whatsappOk, message, 
          emailOk ? "OK" : "FALHA", whatsappOk ? "OK" : "FALHA");
      
    } catch (Exception e) {
      log.error("Erro ao testar serviços", e);
      return new NotificationResponse(false, "Erro ao testar serviços: " + e.getMessage());
    }
  }

  // Métodos auxiliares privados
  
  private NotificationResponse sendToAccounting(NotificationChannel channel, String subject, 
                                               String message, List<byte[]> attachments, List<String> fileNames) {
    return sendToAccounting(channel, subject, message, attachments, fileNames, null, null);
  }
  
  private NotificationResponse sendToAccounting(NotificationChannel channel, String subject, 
                                               String message, List<byte[]> attachments, List<String> fileNames,
                                               String customMessage, String totalValue) {
    boolean emailSent = false;
    boolean whatsappSent = false;

    if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
      emailSent = emailService.sendEmailWithAttachments(accountingEmail, subject, message, attachments, fileNames);
    }

    if (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH) {
      // Para WhatsApp, criar mensagem específica baseada no contexto
      String whatsappMessage = createContextualWhatsAppMessage(message, subject, customMessage, totalValue);
      whatsappSent = whatsAppService.sendMultipleDocuments(accountingWhatsapp, whatsappMessage, attachments, fileNames);
    }

    boolean success = (channel == NotificationChannel.EMAIL && emailSent) ||
                     (channel == NotificationChannel.WHATSAPP && whatsappSent) ||
                     (channel == NotificationChannel.BOTH && emailSent && whatsappSent);

    return NotificationResponse.withStatuses(success, 
        success ? "Enviado com sucesso" : "Falha no envio",
        emailSent ? "OK" : (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH ? "FALHA" : "N/A"),
        whatsappSent ? "OK" : (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH ? "FALHA" : "N/A"));
  }

  private NotificationResponse sendToClient(Client client, NotificationChannel channel, String subject,
                                          String message, List<byte[]> attachments, List<String> fileNames) {
    return sendToClient(client, channel, subject, message, attachments, fileNames, null);
  }
  
  private NotificationResponse sendToClient(Client client, NotificationChannel channel, String subject,
                                          String message, List<byte[]> attachments, List<String> fileNames, String customMessage) {
    boolean emailSent = false;
    boolean whatsappSent = false;

    if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
      if (client.getEmail() != null && !client.getEmail().isEmpty()) {
        emailSent = emailService.sendEmailWithAttachments(client.getEmail(), subject, message, attachments, fileNames);
      }
    }

    if (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH) {
      log.info("=== DEBUG WHATSAPP ===");
      log.info("Cliente ID: {}", client.getId());
      log.info("Cliente Nome: {}", client.getClientName());
      log.info("Número do banco (raw): '{}'", client.getPhoneNumber());
      log.info("Número é null: {}", client.getPhoneNumber() == null);
      log.info("Número é vazio: {}", client.getPhoneNumber() != null ? client.getPhoneNumber().isEmpty() : "N/A");

      if (client.getPhoneNumber() != null && !client.getPhoneNumber().isEmpty()) {
        log.info("Enviando WhatsApp para: '{}'", client.getPhoneNumber());
        // Para WhatsApp, criar mensagem específica para cliente com contexto
        String whatsappMessage = buildWhatsAppClientMessageWithContext(client.getClientName(), customMessage);
        whatsappSent = whatsAppService.sendMultipleDocuments(client.getPhoneNumber(), whatsappMessage, attachments, fileNames);
        log.info("Resultado WhatsApp: {}", whatsappSent);
      } else {
        log.warn("Número de telefone do cliente {} é null ou vazio", client.getClientName());
      }
    }

    boolean success = (channel == NotificationChannel.EMAIL && emailSent) ||
                     (channel == NotificationChannel.WHATSAPP && whatsappSent) ||
                     (channel == NotificationChannel.BOTH && emailSent && whatsappSent);

    return NotificationResponse.withStatuses(success,
        success ? "Enviado para " + client.getClientName() : "Falha no envio para " + client.getClientName(),
        emailSent ? "OK" : (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH ? "FALHA" : "N/A"),
        whatsappSent ? "OK" : (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH ? "FALHA" : "N/A"));
  }

  private String buildMonthlyMessage(MonthlyStatementsRequest request) {
    Map<String, String> variables = new HashMap<>();
    variables.put("MONTH", String.valueOf(request.month()));
    variables.put("YEAR", String.valueOf(request.year()));
    
    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      variables.put("CUSTOM_MESSAGE", request.customMessage());
      variables.put("DEFAULT_MESSAGE", ""); // Não mostrar mensagem padrão
    } else {
      variables.put("CUSTOM_MESSAGE", ""); // Não mostrar observações
      variables.put("DEFAULT_MESSAGE", "true"); // Mostrar mensagem padrão
    }
    
    return emailTemplateService.processTemplate("monthly-statements", variables);
  }

  private String buildGenericFilesMessage(GenericFilesAccountingRequest request, BigDecimal adjustedDebit, BigDecimal adjustedCredit) {
    Map<String, String> variables = new HashMap<>();
    variables.put("DISCOUNTED_VALUE", String.format("%.2f", adjustedCredit));
    
    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      variables.put("CUSTOM_MESSAGE", request.customMessage());
      variables.put("DEFAULT_MESSAGE", ""); // Não mostrar mensagem padrão
    } else {
      variables.put("CUSTOM_MESSAGE", ""); // Não mostrar observações
      variables.put("DEFAULT_MESSAGE", "true"); // Mostrar mensagem padrão
    }
    
    return emailTemplateService.processTemplate("generic-files", variables);
  }

  private String buildClientMessage(ClientDocumentsRequest request, Client client) {
    Map<String, String> variables = new HashMap<>();
    variables.put("CLIENT_NAME", client.getClientName());
    
    // Adicionar data atual
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    variables.put("CURRENT_DATE", today.format(formatter));
    
    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      variables.put("CUSTOM_MESSAGE", request.customMessage());
      variables.put("DEFAULT_MESSAGE", ""); // Não mostrar mensagem padrão
    } else {
      variables.put("CUSTOM_MESSAGE", ""); // Não mostrar mensagem customizada
      variables.put("DEFAULT_MESSAGE", "true"); // Mostrar mensagem padrão
    }
    
    return emailTemplateService.processTemplate("client-documents", variables);
  }

  /**
   * Cria mensagem específica para WhatsApp baseada no contexto da requisição
   */
  private String createContextualWhatsAppMessage(String htmlMessage, String subject, String customMessage, String totalValue) {
    // Determinar tipo baseado no subject
    if (subject != null) {
      if (subject.contains("Documentos Contábeis")) {
        // Para extratos mensais, totalValue é o período (mes/ano)
        return buildWhatsAppMonthlyMessageWithContext(totalValue, customMessage);
      }
      if (subject.contains("Arquivos Contábeis")) {
        // Para arquivos genéricos, totalValue é o valor monetário
        return buildWhatsAppGenericFilesMessageWithContext(customMessage, totalValue);
      }
    }
    
    // Fallback para método original
    return convertHtmlToPlainText(htmlMessage);
  }
  
  private String buildWhatsAppMonthlyMessageWithContext(String period, String customMessage) {
    StringBuilder message = new StringBuilder();
    message.append("Prezados\n\n");
    message.append("Segue anexos contábeis.\n\n");
    message.append("Período: ").append(period != null ? period : "Atual").append("\n\n");
    message.append("Arquivos inclusos:\n");
    message.append("• Extratos bancários (Banco do Brasil e Sicoob)\n");
    message.append("• Planilhas Excel com movimentações financeiras\n");
    message.append("• Notas fiscais do período\n\n");
    
    if (customMessage != null && !customMessage.isEmpty()) {
      message.append("Observações:\n");
      message.append(customMessage).append("\n\n");
    }
    
    message.append("Atenciosamente,\n");
    message.append("Hortifruti SL");
    
    return message.toString();
  }
  
  private String buildWhatsAppGenericFilesMessageWithContext(String customMessage, String totalValue) {
    StringBuilder message = new StringBuilder();
    message.append("Prezados\n\n");
    message.append("Segue anexos contábeis.\n\n");
    
    if (totalValue != null && !totalValue.isEmpty()) {
      message.append("Resumo Financeiro:\n");
      message.append("• Soma dos valores: R$ ").append(totalValue).append("\n\n");
    }
    
    if (customMessage != null && !customMessage.isEmpty()) {
      message.append("Observações:\n");
      message.append(customMessage).append("\n\n");
    }
    
    message.append("Atenciosamente,\n");
    message.append("Hortifruti SL");
    
    return message.toString();
  }
  
  private String buildWhatsAppClientMessageWithContext(String clientName, String customMessage) {
    StringBuilder message = new StringBuilder();
    message.append("Prezados");
    if (clientName != null && !clientName.isEmpty()) {
      message.append(" - ").append(clientName);
    }
    message.append("\n\n");
    message.append("Olá.\n");
    message.append("Por favor verifique os documentos em anexo.\n\n");
    
    if (customMessage != null && !customMessage.isEmpty()) {
      message.append("Observações:\n");
      message.append(customMessage).append("\n\n");
    }
    
    message.append("Atenciosamente,\n");
    message.append("Hortifruti SL");
    
    return message.toString();
  }

  /**
   * Cria mensagem simples para WhatsApp com contexto específico
   */
  private String convertHtmlToPlainText(String htmlMessage) {
    // Extrair contexto da mensagem para criar resposta específica
    String messageType = determineMessageType(htmlMessage);
    
    switch (messageType) {
      case "monthly-statements":
        return buildWhatsAppMonthlyMessage(htmlMessage);
      case "generic-files":
        return buildWhatsAppGenericFilesMessage(htmlMessage);
      case "client-documents":
        return buildWhatsAppClientMessage(htmlMessage);
      default:
        return "Por favor verifique os documentos em anexo.\n\nAtenciosamente,\nHortifruti SL";
    }
  }
  
  private String determineMessageType(String htmlMessage) {
    if (htmlMessage == null) return "default";
    
    if (htmlMessage.contains("Extratos Mensais") || htmlMessage.contains("monthly-statements")) {
      return "monthly-statements";
    }
    if (htmlMessage.contains("arquivos genéricos") || htmlMessage.contains("generic-files")) {
      return "generic-files";
    }
    if (htmlMessage.contains("client-documents")) {
      return "client-documents";
    }
    return "default";
  }
  
  private String buildWhatsAppMonthlyMessage(String htmlMessage) {
    // Extrair mês/ano do HTML
    String period = extractPeriod(htmlMessage);
    
    return "Prezados\n\n" +
           "Segue anexos contábeis.\n\n" +
           "Período: " + period + "\n\n" +
           "Arquivos inclusos:\n" +
           "• Extratos bancários (Banco do Brasil e Sicoob)\n" +
           "• Planilhas Excel com movimentações financeiras\n" +
           "• Notas fiscais do período\n\n" +
           "Atenciosamente,\n" +
           "Hortifruti SL";
  }
  
  private String buildWhatsAppGenericFilesMessage(String htmlMessage) {
    // Extrair valor total se disponível
    String totalValue = extractTotalValue(htmlMessage);
    String customMessage = extractCustomMessage(htmlMessage);
    
    StringBuilder message = new StringBuilder();
    message.append("Prezados\n\n");
    message.append("Segue anexos contábeis.\n\n");
    
    if (!totalValue.isEmpty()) {
      message.append("Resumo Financeiro:\n");
      message.append("• Soma dos valores: R$ ").append(totalValue).append("\n\n");
    }
    
    if (!customMessage.isEmpty()) {
      message.append("Observações:\n");
      message.append(customMessage).append("\n\n");
    }
    
    message.append("Atenciosamente,\n");
    message.append("Hortifruti SL");
    
    return message.toString();
  }
  
  private String buildWhatsAppClientMessage(String htmlMessage) {
    String clientName = extractClientName(htmlMessage);
    String customMessage = extractCustomMessage(htmlMessage);
    
    StringBuilder message = new StringBuilder();
    message.append("Prezados");
    if (!clientName.isEmpty()) {
      message.append(" - ").append(clientName);
    }
    message.append("\n\n");
    message.append("Por favor verifique os documentos em anexo..\n\n");
    
    if (!customMessage.isEmpty()) {
      message.append("Observações:\n");
      message.append(customMessage).append("\n\n");
    }
    
    message.append("Atenciosamente,\n");
    message.append("Hortifruti SL");
    
    return message.toString();
  }
  
  private String extractPeriod(String htmlMessage) {
    // Procurar por padrões de data como "10/2025"
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{1,2}/\\d{4})");
    java.util.regex.Matcher matcher = pattern.matcher(htmlMessage);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "Atual";
  }
  
  private String extractTotalValue(String htmlMessage) {
    // Procurar por valores monetários
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("R\\$\\s*([\\d.,]+)");
    java.util.regex.Matcher matcher = pattern.matcher(htmlMessage);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "";
  }
  
  private String extractCustomMessage(String htmlMessage) {
    // Extrair mensagem personalizada entre tags específicas
    if (htmlMessage.contains("{{CUSTOM_MESSAGE}}")) {
      // Buscar o valor da variável CUSTOM_MESSAGE no contexto
      return ""; // Por enquanto vazio, será preenchido pelo contexto da requisição
    }
    
    // Procurar por mensagens entre tags p que não sejam padrão
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<p[^>]*>([^<]+)</p>");
    java.util.regex.Matcher matcher = pattern.matcher(htmlMessage);
    while (matcher.find()) {
      String text = matcher.group(1).trim();
      if (!text.isEmpty() && 
          !text.contains("Prezados") && 
          !text.contains("Atenciosamente") && 
          !text.contains("anexo") &&
          !text.contains("{{")) {
        return text;
      }
    }
    return "";
  }
  
  private String extractClientName(String htmlMessage) {
    // Procurar por nome do cliente
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("{{CLIENT_NAME}}|Cliente[:\\s]+([^<\\n]+)");
    java.util.regex.Matcher matcher = pattern.matcher(htmlMessage);
    if (matcher.find() && matcher.groupCount() > 0) {
      return matcher.group(1);
    }
    return "";
  }


}
