package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.exception.NotificationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Fachada de envio de email. Delega para o {@link EmailSender} configurado em
 * {@code email.provider} (sendgrid | gmail | gmail-api), sem expor detalhes do provedor aos
 * chamadores.
 *
 * <p>Para trocar de provedor basta alterar a variável de ambiente {@code EMAIL_PROVIDER} — nenhum
 * outro código precisa mudar. Isso permite manter os outros provedores intactos mesmo quando um
 * deles está ativo, sem quebrar quem já depende de {@link EmailService}.
 */
@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

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
    EmailSender sender = resolveSender();
    log.info(
        "[Email] provider={} sendSimpleEmail to={} subject={}", sender.providerName(), to, subject);
    try {
      boolean result = sender.sendSimpleEmail(to, subject, text);
      log.info(
          "[Email] provider={} sendSimpleEmail concluído com sucesso to={}",
          sender.providerName(),
          to);
      return result;
    } catch (NotificationException e) {
      log.error(
          "[Email] provider={} sendSimpleEmail FALHOU to={}: {}",
          sender.providerName(),
          to,
          e.getMessage());
      throw e;
    }
  }

  public boolean sendEmailWithAttachments(
      String to, String subject, String text, List<byte[]> attachments, List<String> fileNames) {
    EmailSender sender = resolveSender();
    log.info(
        "[Email] provider={} sendEmailWithAttachments to={} subject={} anexos={}",
        sender.providerName(),
        to,
        subject,
        attachments == null ? 0 : attachments.size());
    try {
      boolean result = sender.sendEmailWithAttachments(to, subject, text, attachments, fileNames);
      log.info(
          "[Email] provider={} sendEmailWithAttachments concluído com sucesso to={}",
          sender.providerName(),
          to);
      return result;
    } catch (NotificationException e) {
      log.error(
          "[Email] provider={} sendEmailWithAttachments FALHOU to={}: {}",
          sender.providerName(),
          to,
          e.getMessage());
      throw e;
    }
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
        "[Email] Valor de email.provider='{}' não reconhecido (use 'sendgrid', 'gmail' ou"
            + " 'gmail-api'). Usando SendGrid como padrão.",
        provider);
    return sendGridEmailSender;
  }
}
