package com.hortifruti.sl.hortifruti.service.notification;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Fachada de envio de email. Delega para o {@link EmailSender} configurado em {@code
 * email.provider} (sendgrid | gmail | gmail-api), sem expor detalhes do provedor aos chamadores.
 *
 * <p>Para trocar de provedor basta alterar a variável de ambiente {@code EMAIL_PROVIDER} — nenhum
 * outro código precisa mudar. Isso permite manter os outros provedores intactos mesmo quando um
 * deles está ativo, sem quebrar quem já depende de {@link EmailService}.
 */
@Slf4j
@Service
public class EmailService {

  @Value("${email.provider:sendgrid}")
  private String provider;

  private final SendGridEmailSender sendGridEmailSender;
  private final GmailSmtpEmailSender gmailSmtpEmailSender;
  private final GmailApiEmailSender gmailApiEmailSender;

  public EmailService(
      SendGridEmailSender sendGridEmailSender,
      GmailSmtpEmailSender gmailSmtpEmailSender,
      GmailApiEmailSender gmailApiEmailSender) {
    this.sendGridEmailSender = sendGridEmailSender;
    this.gmailSmtpEmailSender = gmailSmtpEmailSender;
    this.gmailApiEmailSender = gmailApiEmailSender;
  }

  public boolean sendSimpleEmail(String to, String subject, String text) {
    return resolveSender().sendSimpleEmail(to, subject, text);
  }

  public boolean sendEmailWithAttachments(
      String to, String subject, String text, List<byte[]> attachments, List<String> fileNames) {
    return resolveSender().sendEmailWithAttachments(to, subject, text, attachments, fileNames);
  }

  public boolean sendEmailWithSingleAttachment(
      String to, String subject, String text, byte[] attachment, String fileName) {
    return sendEmailWithAttachments(to, subject, text, List.of(attachment), List.of(fileName));
  }

  private EmailSender resolveSender() {
    if ("gmail".equalsIgnoreCase(provider)) {
      return gmailSmtpEmailSender;
    }
    if ("gmail-api".equalsIgnoreCase(provider)) {
      return gmailApiEmailSender;
    }
    if ("sendgrid".equalsIgnoreCase(provider)) {
      return sendGridEmailSender;
    }
    log.warn(
        "email.provider='{}' não reconhecido (use 'sendgrid', 'gmail' ou 'gmail-api'). Usando"
            + " SendGrid como padrão.",
        provider);
    return sendGridEmailSender;
  }
}
