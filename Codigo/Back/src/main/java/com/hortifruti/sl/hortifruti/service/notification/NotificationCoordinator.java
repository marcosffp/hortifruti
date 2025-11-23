package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.NotificationResponse;
import com.hortifruti.sl.hortifruti.exception.NotificationException;
import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    boolean emailSent = false;
    boolean whatsappSent = false;

    // EMAIL
    if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
      if (emailRecipient == null || emailRecipient.trim().isEmpty()) {
        throw new NotificationException("Destinatário de email inválido.");
      }
      emailSent =
          emailService.sendEmailWithAttachments(
              emailRecipient, subject, emailBody, attachments, fileNames);
    }

    // WHATSAPP
    if (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH) {
      if (whatsAppRecipient == null || whatsAppRecipient.trim().isEmpty()) {
        throw new NotificationException("Destinatário de WhatsApp inválido.");
      }
      String whatsappMessage = buildWhatsAppMessage(whatsAppType, whatsAppContext);
      whatsappSent =
          whatsAppService.sendMultipleDocuments(
              whatsAppRecipient, whatsappMessage, attachments, fileNames);
    }

    boolean success = determineSuccess(channel, emailSent, whatsappSent);

    return NotificationResponse.withStatuses(
        success,
        success ? "Enviado com sucesso" : "Falha no envio",
        getEmailStatus(channel, emailSent),
        getWhatsAppStatus(channel, whatsappSent));
  }

  /** Constrói mensagem de WhatsApp baseada no tipo e contexto */
  private String buildWhatsAppMessage(WhatsAppMessageType type, WhatsAppMessageContext context) {
    switch (type) {
      case MONTHLY_STATEMENTS:
        return whatsAppMessageBuilder.buildMonthlyStatementsMessage(
            context.getPeriod(), context.getCustomMessage());

      case GENERIC_FILES:
        return whatsAppMessageBuilder.buildGenericFilesMessage(
            context.getTotalValue(), context.getCustomMessage());

      case CLIENT_DOCUMENTS:
        return whatsAppMessageBuilder.buildClientDocumentsMessage(
            context.getClient(), context.getCustomMessage());

      case GENERIC:
      default:
        return whatsAppMessageBuilder.buildGenericMessage(
            context.getSubject(), context.getCustomMessage());
    }
  }

  private boolean determineSuccess(
      NotificationChannel channel, boolean emailSent, boolean whatsappSent) {
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

    if (recipient == null || recipient.trim().isEmpty()) {
      throw new NotificationException("Destinatário de email inválido.");
    }

    try {
      if (attachments != null && !attachments.isEmpty()) {
        return emailService.sendEmailWithAttachments(
            recipient, subject, body, attachments, fileNames);
      } else {
        return emailService.sendSimpleEmail(recipient, subject, body);
      }
    } catch (Exception e) {
      throw new NotificationException("Erro ao enviar email: " + e.getMessage());
    }
  }

  private String getEmailStatus(NotificationChannel channel, boolean emailSent) {
    if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
      return emailSent ? "OK" : "FALHA";
    }
    return "N/A";
  }

  private String getWhatsAppStatus(NotificationChannel channel, boolean whatsappSent) {
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
