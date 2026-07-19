package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.exception.NotificationException;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Envio de email via SendGrid API. Ativado quando {@code email.provider=sendgrid}. */
@Component
public class SendGridEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(SendGridEmailSender.class);

  @Value("${sendgrid.api.key:}")
  private String sendGridApiKey;

  @Value("${sendgrid.from.email:}")
  private String fromEmail;

  @Override
  public String providerName() {
    return "sendgrid";
  }

  @Override
  public boolean sendSimpleEmail(String to, String subject, String text) {
    return doSend(to, subject, text, null, null);
  }

  @Override
  public boolean sendEmailWithAttachments(
      String to, String subject, String text, List<byte[]> attachments, List<String> fileNames) {
    return doSend(to, subject, text, attachments, fileNames);
  }

  private boolean doSend(
      String to, String subject, String text, List<byte[]> attachments, List<String> fileNames) {
    long start = System.currentTimeMillis();
    log.info(
        "[SendGrid] Iniciando envio de email to={} subject={} anexos={} fromConfigurado={}",
        to,
        subject,
        attachments == null ? 0 : attachments.size(),
        fromEmail != null && !fromEmail.isBlank());

    if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
      log.error(
          "[SendGrid] SENDGRID_API_KEY não configurada. Verifique a variável de ambiente antes de"
              + " tentar enviar email.");
      throw new NotificationException("SendGrid API key não configurada");
    }
    if (fromEmail == null || fromEmail.isBlank()) {
      log.error(
          "[SendGrid] SENDGRID_FROM_EMAIL não configurado. Verifique a variável de ambiente antes"
              + " de tentar enviar email.");
      throw new NotificationException("SendGrid from email não configurado");
    }

    try {
      Email from = new Email(fromEmail);
      Email toEmail = new Email(to);
      Content content = new Content("text/html", text);
      Mail mail = new Mail(from, subject, toEmail, content);

      addInlineLogo(mail);

      if (attachments != null && fileNames != null) {
        for (int i = 0; i < attachments.size() && i < fileNames.size(); i++) {
          Attachments attachment = new Attachments();
          String encodedFile = Base64.getEncoder().encodeToString(attachments.get(i));
          attachment.setContent(encodedFile);
          attachment.setFilename(fileNames.get(i));
          attachment.setDisposition("attachment");
          mail.addAttachments(attachment);
        }
      }

      SendGrid sg = new SendGrid(sendGridApiKey);
      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());

      log.debug("[SendGrid] Enviando requisição POST mail/send para to={}", to);
      Response response = sg.api(request);
      long elapsed = System.currentTimeMillis() - start;

      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        log.info(
            "[SendGrid] Email enviado com sucesso to={} status={} em {}ms",
            to,
            response.getStatusCode(),
            elapsed);
        return true;
      } else {
        log.error(
            "[SendGrid] Falha ao enviar email to={} status={} body={} em {}ms",
            to,
            response.getStatusCode(),
            response.getBody(),
            elapsed);
        throw new NotificationException(
            "Erro ao enviar email. Status: " + response.getStatusCode());
      }

    } catch (IOException e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error(
          "[SendGrid] Exceção de IO ao enviar email to={} apos {}ms: {}",
          to,
          elapsed,
          e.getMessage(),
          e);
      throw new NotificationException("Falha ao enviar email: " + e.getMessage(), e);
    }
  }

  private void addInlineLogo(Mail mail) {
    try {
      ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");

      if (logoResource.exists()) {
        byte[] logoBytes = logoResource.getInputStream().readAllBytes();
        String encodedLogo = Base64.getEncoder().encodeToString(logoBytes);

        Attachments logo = new Attachments();
        logo.setContent(encodedLogo);
        logo.setType("image/png");
        logo.setFilename("logo.png");
        logo.setDisposition("inline");
        logo.setContentId("logo");

        mail.addAttachments(logo);
      } else {
        log.warn("[SendGrid] Logo inline static/images/logo.png não encontrado no classpath.");
      }

    } catch (IOException e) {
      log.warn("[SendGrid] Falha ao anexar logo inline: {}", e.getMessage());
    }
  }
}
