package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.NotificationResponse;
import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service de coordenação para envio de notificações Responsável por orquestrar Email e WhatsApp de
 * forma independente
 */
@Service
@RequiredArgsConstructor
public class NotificationCoordinator {

  private final EmailService emailService;
  private final WhatsAppService whatsAppService;
  private final WhatsAppMessageBuilder whatsAppMessageBuilder;

  /** Envia notificação para um destinatário específico */
  public NotificationResponse sendNotification(
      String recipient,
      NotificationChannel channel,
      String subject,
      String emailBody,
      WhatsAppMessageType whatsAppType,
      WhatsAppMessageContext whatsAppContext,
      List<byte[]> attachments,
      List<String> fileNames) {

    System.out.println("Iniciando envio de notificação para destinatário único...");
    return sendNotification(
        recipient,
        recipient,
        channel,
        subject,
        emailBody,
        whatsAppType,
        whatsAppContext,
        attachments,
        fileNames);
  }

  /** Envia notificação com destinatários diferentes para email e WhatsApp */
  public NotificationResponse sendNotification(
      String emailRecipient,
      String whatsAppRecipient,
      NotificationChannel channel,
      String subject,
      String emailBody,
      WhatsAppMessageType whatsAppType,
      WhatsAppMessageContext whatsAppContext,
      List<byte[]> attachments,
      List<String> fileNames) {

    System.out.println("Iniciando envio de notificação...");
    System.out.println("Destinatário de email: " + emailRecipient);
    System.out.println("Destinatário de WhatsApp: " + whatsAppRecipient);
    System.out.println("Canal: " + channel);
    System.out.println("Assunto: " + subject);

    boolean emailSent = false;
    boolean whatsappSent = false;

    // Envio por Email
    if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
      System.out.println("Tentando enviar notificação por email...");
      if (emailRecipient != null && !emailRecipient.trim().isEmpty()) {
        emailSent =
            emailService.sendEmailWithAttachments(
                emailRecipient, subject, emailBody, attachments, fileNames);
        System.out.println("Resultado do envio de email: " + emailSent);
      } else {
        System.out.println("Destinatário de email não fornecido ou inválido.");
      }
    }

    // Envio por WhatsApp (independente do email)
    if (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH) {
      System.out.println("Tentando enviar notificação por WhatsApp...");
      if (whatsAppRecipient != null && !whatsAppRecipient.trim().isEmpty()) {
        String whatsappMessage = buildWhatsAppMessage(whatsAppType, whatsAppContext);
        System.out.println("Mensagem de WhatsApp construída: " + whatsappMessage);
        whatsappSent =
            whatsAppService.sendMultipleDocuments(
                whatsAppRecipient, whatsappMessage, attachments, fileNames);
        System.out.println("Resultado do envio de WhatsApp: " + whatsappSent);
      } else {
        System.out.println("Destinatário de WhatsApp não fornecido ou inválido.");
      }
    }

    // Determinar sucesso baseado no canal solicitado
    boolean success = determineSuccess(channel, emailSent, whatsappSent);
    System.out.println("Resultado final do envio: " + (success ? "Sucesso" : "Falha"));

    return NotificationResponse.withStatuses(
        success,
        success ? "Enviado com sucesso" : "Falha no envio",
        getEmailStatus(channel, emailSent),
        getWhatsAppStatus(channel, whatsappSent));
  }

  /** Constrói mensagem de WhatsApp baseada no tipo e contexto */
  private String buildWhatsAppMessage(WhatsAppMessageType type, WhatsAppMessageContext context) {
    System.out.println("Construindo mensagem de WhatsApp...");
    switch (type) {
      case MONTHLY_STATEMENTS:
        System.out.println("Tipo de mensagem: MONTHLY_STATEMENTS");
        return whatsAppMessageBuilder.buildMonthlyStatementsMessage(
            context.getPeriod(), context.getCustomMessage());

      case GENERIC_FILES:
        System.out.println("Tipo de mensagem: GENERIC_FILES");
        return whatsAppMessageBuilder.buildGenericFilesMessage(
            context.getTotalValue(), context.getCustomMessage());

      case CLIENT_DOCUMENTS:
        System.out.println("Tipo de mensagem: CLIENT_DOCUMENTS");
        return whatsAppMessageBuilder.buildClientDocumentsMessage(
            context.getClient(), context.getCustomMessage());

      case GENERIC:
      default:
        System.out.println("Tipo de mensagem: GENERIC");
        return whatsAppMessageBuilder.buildGenericMessage(
            context.getSubject(), context.getCustomMessage());
    }
  }

  private boolean determineSuccess(
      NotificationChannel channel, boolean emailSent, boolean whatsappSent) {
    System.out.println("Determinando sucesso do envio...");
    switch (channel) {
      case EMAIL:
        return emailSent;
      case WHATSAPP:
        return whatsappSent;
      case BOTH:
        return emailSent && whatsappSent;
      default:
        return false;
    }
  }

  /** Envia apenas email (sem WhatsApp) */
  public boolean sendEmailOnly(
      String recipient,
      String subject,
      String body,
      List<byte[]> attachments,
      List<String> fileNames) {
    System.out.println("Enviando apenas email...");
    System.out.println("Destinatário: " + recipient);
    System.out.println("Assunto: " + subject);
    try {
      if (attachments != null && !attachments.isEmpty()) {
        System.out.println("Anexos detectados. Enviando email com anexos...");
        return emailService.sendEmailWithAttachments(
            recipient, subject, body, attachments, fileNames);
      } else {
        System.out.println("Sem anexos. Enviando email simples...");
        return emailService.sendSimpleEmail(recipient, subject, body);
      }
    } catch (Exception e) {
      System.out.println("Erro ao enviar email: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private String getEmailStatus(NotificationChannel channel, boolean emailSent) {
    System.out.println("Obtendo status do email...");
    if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
      return emailSent ? "OK" : "FALHA";
    }
    return "N/A";
  }

  private String getWhatsAppStatus(NotificationChannel channel, boolean whatsappSent) {
    System.out.println("Obtendo status do WhatsApp...");
    if (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH) {
      return whatsappSent ? "OK" : "FALHA";
    }
    return "N/A";
  }

  /** Enum para tipos de mensagem WhatsApp */
  public enum WhatsAppMessageType {
    MONTHLY_STATEMENTS,
    GENERIC_FILES,
    CLIENT_DOCUMENTS,
    GENERIC
  }

  /** Classe para contexto das mensagens WhatsApp */
  public static class WhatsAppMessageContext {
    private String period;
    private String totalValue;
    private String clientName;
    private String customMessage;
    private String subject;
    private Client client;

    // Builder pattern
    public static WhatsAppMessageContext builder() {
      return new WhatsAppMessageContext();
    }

    public WhatsAppMessageContext period(String period) {
      this.period = period;
      return this;
    }

    public WhatsAppMessageContext totalValue(String totalValue) {
      this.totalValue = totalValue;
      return this;
    }

    public WhatsAppMessageContext clientName(String clientName) {
      this.clientName = clientName;
      return this;
    }

    public WhatsAppMessageContext customMessage(String customMessage) {
      this.customMessage = customMessage;
      return this;
    }

    public WhatsAppMessageContext subject(String subject) {
      this.subject = subject;
      return this;
    }

    public WhatsAppMessageContext client(Client client) {
      this.client = client;
      return this;
    }

    // Getters
    public String getPeriod() {
      return period;
    }

    public String getTotalValue() {
      return totalValue;
    }

    public String getClientName() {
      return clientName;
    }

    public String getCustomMessage() {
      return customMessage;
    }

    public String getSubject() {
      return subject;
    }

    public Client getClient() {
      return client;
    }
  }
}
