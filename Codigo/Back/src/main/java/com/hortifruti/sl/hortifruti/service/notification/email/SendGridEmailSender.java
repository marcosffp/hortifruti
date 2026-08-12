package com.hortifruti.sl.hortifruti.service.notification.email;

import com.hortifruti.sl.hortifruti.exception.notification.NotificationException;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Envio de email via SendGrid API. Ativado quando {@code email.provider=sendgrid}. */
@Slf4j
@Component
public class SendGridEmailSender implements EmailSender {

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
    if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
      throw new NotificationException("SendGrid API key não configurada");
    }
    if (fromEmail == null || fromEmail.isBlank()) {
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

      Response response = sg.api(request);

      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        return true;
      } else {
        log.error(
            "SendGrid recusou o envio para {}: status={} body={}",
            to,
            response.getStatusCode(),
            response.getBody());
        throw new NotificationException(
            "Erro ao enviar email. Status: " + response.getStatusCode());
      }

    } catch (IOException e) {
      log.error("Falha de IO ao enviar email via SendGrid para {}: {}", to, e.getMessage());
      throw new NotificationException("Falha ao enviar email: " + e.getMessage(), e);
    }
  }

  private void addInlineLogo(Mail mail) {
    InlineLogoAttacher.readLogoBytes()
        .ifPresent(
            logoBytes -> {
              Attachments logo = new Attachments();
              logo.setContent(Base64.getEncoder().encodeToString(logoBytes));
              logo.setType("image/png");
              logo.setFilename("logo.png");
              logo.setDisposition("inline");
              logo.setContentId(InlineLogoAttacher.CONTENT_ID);

              mail.addAttachments(logo);
            });
  }
}
