package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.exception.NotificationException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envio de email via SMTP do Gmail. Ativado quando {@code email.provider=gmail}.
 *
 * <p>Loga extensivamente cada etapa da conexão/envio para facilitar o diagnóstico em produção,
 * onde bloqueios de rede/firewall na porta SMTP costumam se manifestar como timeouts silenciosos.
 */
@Component
public class GmailSmtpEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(GmailSmtpEmailSender.class);

  @Value("${gmail.smtp.host:smtp.gmail.com}")
  private String host;

  @Value("${gmail.smtp.port:587}")
  private int port;

  @Value("${GMAIL:}")
  private String username;

  @Value("${GMAIL_PASSWORD:}")
  private String appPassword;

  @Value("${gmail.smtp.connection-timeout-ms:10000}")
  private int connectionTimeoutMs;

  @Value("${gmail.smtp.timeout-ms:10000}")
  private int timeoutMs;

  @Value("${email.provider:sendgrid}")
  private String activeProvider;

  private volatile JavaMailSenderImpl mailSender;

  @Override
  public String providerName() {
    return "gmail";
  }

  @PostConstruct
  void logConfigAndTestConnectionOnStartup() {
    if (username == null || username.isBlank()) {
      log.info(
          "[Gmail SMTP] GMAIL não configurado — provedor Gmail não será testado no startup.");
      return;
    }

    log.info(
        "[Gmail SMTP] Configuração carregada: host={} port={} username={} providerAtivo={}"
            + " connectionTimeoutMs={} timeoutMs={}",
        host,
        port,
        maskEmail(username),
        activeProvider,
        connectionTimeoutMs,
        timeoutMs);

    if (!"gmail".equalsIgnoreCase(activeProvider)) {
      log.info(
          "[Gmail SMTP] email.provider={} — pulando teste de conectividade no startup (Gmail não é"
              + " o provedor ativo).",
          activeProvider);
      return;
    }

    try {
      long start = System.currentTimeMillis();
      log.info(
          "[Gmail SMTP] Testando conectividade/autenticação com {}:{} antes de liberar o"
              + " envio de emails...",
          host,
          port);
      getMailSender().testConnection();
      log.info(
          "[Gmail SMTP] Teste de conectividade OK ({}ms). Host {}:{} alcançável e credenciais"
              + " aceitas.",
          System.currentTimeMillis() - start,
          host,
          port);
    } catch (Exception e) {
      log.error(
          "[Gmail SMTP] Teste de conectividade FALHOU ao subir a aplicação. Isso geralmente indica"
              + " que a porta {} para {} está bloqueada pelo firewall/host de deploy, ou que"
              + " GMAIL/GMAIL_PASSWORD estão incorretos (lembre-se: GMAIL_PASSWORD deve ser uma"
              + " 'App Password' do Google, não a senha normal da conta). Detalhe: {}",
          port,
          host,
          e.getMessage(),
          e);
    }
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
    props.put("mail.smtp.starttls.enable", "true");
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
    long start = System.currentTimeMillis();

    if (username == null || username.isBlank() || appPassword == null || appPassword.isBlank()) {
      log.error(
          "[Gmail SMTP] GMAIL/GMAIL_PASSWORD não configurados. Verifique as variáveis de ambiente"
              + " antes de tentar enviar email.");
      throw new NotificationException("Credenciais do Gmail não configuradas (GMAIL/GMAIL_PASSWORD)");
    }

    log.info(
        "[Gmail SMTP] Iniciando envio to={} subject={} anexos={} host={} port={}",
        to,
        subject,
        attachments == null ? 0 : attachments.size(),
        host,
        port);

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
          helper.addAttachment(
              fileNames.get(i), new org.springframework.core.io.ByteArrayResource(attachments.get(i)));
        }
      }

      log.debug("[Gmail SMTP] Conectando a {}:{} para enviar mensagem to={}", host, port, to);
      sender.send(message);

      long elapsed = System.currentTimeMillis() - start;
      log.info("[Gmail SMTP] Email enviado com sucesso to={} em {}ms", to, elapsed);
      return true;

    } catch (MailAuthenticationException e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error(
          "[Gmail SMTP] Falha de AUTENTICAÇÃO ao enviar email to={} apos {}ms. Verifique se"
              + " GMAIL_PASSWORD é uma App Password válida do Google (conta com verificação em"
              + " duas etapas habilitada) e não a senha normal. Detalhe: {}",
          to,
          elapsed,
          e.getMessage(),
          e);
      throw new NotificationException("Falha de autenticação no Gmail SMTP: " + e.getMessage(), e);

    } catch (MailSendException e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error(
          "[Gmail SMTP] Falha ao ENVIAR/CONECTAR to={} apos {}ms via {}:{}. Se o ambiente de"
              + " deploy bloquear a porta SMTP (comum em alguns provedores de hospedagem), este é"
              + " o sintoma esperado — considere testar com 'telnet {} {}' a partir do servidor."
              + " Detalhe: {}",
          to,
          elapsed,
          host,
          port,
          host,
          port,
          e.getMessage(),
          e);
      throw new NotificationException("Falha ao enviar email via Gmail SMTP: " + e.getMessage(), e);

    } catch (MailException e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error(
          "[Gmail SMTP] Erro inesperado do Spring Mail to={} apos {}ms: {}",
          to,
          elapsed,
          e.getMessage(),
          e);
      throw new NotificationException("Falha ao enviar email via Gmail SMTP: " + e.getMessage(), e);

    } catch (MessagingException e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error(
          "[Gmail SMTP] Erro ao montar a mensagem MIME to={} apos {}ms: {}",
          to,
          elapsed,
          e.getMessage(),
          e);
      throw new NotificationException("Falha ao montar email para Gmail SMTP: " + e.getMessage(), e);
    }
  }

  private void addInlineLogo(MimeMessageHelper helper) {
    try {
      ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
      if (logoResource.exists()) {
        helper.addInline("logo", logoResource);
      } else {
        log.warn("[Gmail SMTP] Logo inline static/images/logo.png não encontrado no classpath.");
      }
    } catch (MessagingException e) {
      log.warn("[Gmail SMTP] Falha ao anexar logo inline: {}", e.getMessage());
    }
  }

  private static String maskEmail(String email) {
    int at = email.indexOf('@');
    if (at <= 1) {
      return "***";
    }
    return email.charAt(0) + "***" + email.substring(at);
  }
}
