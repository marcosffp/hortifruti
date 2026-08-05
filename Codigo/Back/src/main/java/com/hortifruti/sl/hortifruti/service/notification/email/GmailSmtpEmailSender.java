package com.hortifruti.sl.hortifruti.service.notification.email;

import com.hortifruti.sl.hortifruti.exception.notification.NotificationException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Envio de email via SMTP do Gmail. Ativado quando {@code email.provider=gmail}. */
@Slf4j
@Component
public class GmailSmtpEmailSender implements EmailSender {

  @Value("${gmail.smtp.host:smtp.gmail.com}")
  private String host;

  @Value("${gmail.smtp.port:587}")
  private int port;

  @Value("${username.gmail}")
  private String username;

  @Value("${password.gmail}")
  private String appPassword;

  @Value("${gmail.smtp.connection-timeout-ms:10000}")
  private int connectionTimeoutMs;

  @Value("${gmail.smtp.timeout-ms:10000}")
  private int timeoutMs;

  private volatile JavaMailSenderImpl mailSender;

  @Override
  public String providerName() {
    return "gmail";
  }

  private JavaMailSenderImpl getMailSender() {
    JavaMailSenderImpl instance = mailSender;
    if (instance == null) {
      synchronized (this) {
        instance = mailSender;
        if (instance == null) {
          instance = buildMailSender();
          mailSender = instance;
        }
      }
    }
    return instance;
  }

  private JavaMailSenderImpl buildMailSender() {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    sender.setUsername(username);
    sender.setPassword(appPassword);

    Properties props = sender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    if (port == 465) {
      // Porta 465 usa SSL implícito desde a conexão inicial (sem STARTTLS).
      props.put("mail.smtp.ssl.enable", "true");
    } else {
      props.put("mail.smtp.starttls.enable", "true");
    }
    props.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeoutMs));
    props.put("mail.smtp.timeout", String.valueOf(timeoutMs));
    props.put("mail.smtp.writetimeout", String.valueOf(timeoutMs));
    return sender;
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
    if (username == null || username.isBlank() || appPassword == null || appPassword.isBlank()) {
      throw new NotificationException(
          "Credenciais do Gmail não configuradas (GMAIL/GMAIL_PASSWORD)");
    }

    try {
      JavaMailSenderImpl sender = getMailSender();
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(username);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, true);

      addInlineLogo(helper);

      if (attachments != null && fileNames != null) {
        for (int i = 0; i < attachments.size() && i < fileNames.size(); i++) {
          helper.addAttachment(fileNames.get(i), new ByteArrayResource(attachments.get(i)));
        }
      }

      sender.send(message);
      return true;

    } catch (MailAuthenticationException e) {
      log.error("Falha de autenticação no Gmail SMTP para {}: {}", to, e.getMessage());
      throw new NotificationException("Falha de autenticação no Gmail SMTP: " + e.getMessage(), e);

    } catch (MailSendException e) {
      log.error("Falha ao enviar/conectar via Gmail SMTP para {}: {}", to, e.getMessage());
      throw new NotificationException("Falha ao enviar email via Gmail SMTP: " + e.getMessage(), e);

    } catch (MailException e) {
      log.error("Erro do Spring Mail ao enviar para {}: {}", to, e.getMessage());
      throw new NotificationException("Falha ao enviar email via Gmail SMTP: " + e.getMessage(), e);

    } catch (MessagingException e) {
      log.error("Erro ao montar mensagem MIME para {}: {}", to, e.getMessage());
      throw new NotificationException(
          "Falha ao montar email para Gmail SMTP: " + e.getMessage(), e);
    }
  }

  private void addInlineLogo(MimeMessageHelper helper) {
    try {
      ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
      if (logoResource.exists()) {
        helper.addInline("logo", logoResource);
      }
    } catch (MessagingException e) {
      log.warn("Não foi possível anexar o logo inline ao email: {}", e.getMessage());
    }
  }
}
