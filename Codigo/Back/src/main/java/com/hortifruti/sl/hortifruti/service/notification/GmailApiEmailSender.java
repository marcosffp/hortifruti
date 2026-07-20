package com.hortifruti.sl.hortifruti.service.notification;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.BackupException;
import com.hortifruti.sl.hortifruti.exception.NotificationException;
import com.hortifruti.sl.hortifruti.service.backup.auth.CredentialConfig;
import com.hortifruti.sl.hortifruti.service.backup.auth.CredentialManager;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envio de email via Gmail API (HTTPS/443), usando o mesmo fluxo OAuth já usado para o backup no
 * Google Drive. Ativado quando {@code email.provider=gmail-api}.
 *
 * <p>Diferente do {@link GmailSmtpEmailSender}, este não abre conexão SMTP direta — chama a API
 * REST do Google, então não é afetado por bloqueios de porta SMTP (587/465) comuns em hosts de
 * deploy como Railway.
 */
@Component
public class GmailApiEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(GmailApiEmailSender.class);
  private static final String APPLICATION_NAME = "Hortifruti SL Notificacoes";

  private final Base64FileDecoder base64FileDecoder;
  private final CredentialManager credentialManager;

  @Value("${google.redirect.uri}")
  private String redirectUri;

  @Value("${google.tokens.directory}")
  private String tokensDirectoryPath;

  @Value("${GMAIL:}")
  private String senderAddress;

  public GmailApiEmailSender(
      Base64FileDecoder base64FileDecoder, CredentialManager credentialManager) {
    this.base64FileDecoder = base64FileDecoder;
    this.credentialManager = credentialManager;
  }

  @Override
  public String providerName() {
    return "gmail-api";
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

    if (senderAddress == null || senderAddress.isBlank()) {
      log.error("[Gmail API] GMAIL não configurado. Verifique a variável de ambiente.");
      throw new NotificationException("Conta do Gmail não configurada (GMAIL)");
    }

    log.info(
        "[Gmail API] Iniciando envio to={} subject={} anexos={} conta={}",
        to,
        subject,
        attachments == null ? 0 : attachments.size(),
        maskEmail(senderAddress));

    try {
      Gmail gmail = buildGmailClient();
      MimeMessage mimeMessage = buildMimeMessage(to, subject, text, attachments, fileNames);
      Message message = toGmailMessage(mimeMessage);

      log.debug("[Gmail API] Chamando users().messages().send() to={}", to);
      Message sent = gmail.users().messages().send("me", message).execute();

      long elapsed = System.currentTimeMillis() - start;
      log.info(
          "[Gmail API] Email enviado com sucesso to={} messageId={} em {}ms",
          to,
          sent.getId(),
          elapsed);
      return true;

    } catch (BackupException e) {
      long elapsed = System.currentTimeMillis() - start;
      if (e.getMessage() != null && e.getMessage().startsWith("AUTHORIZATION_REQUIRED:")) {
        String authUrl = e.getMessage().substring("AUTHORIZATION_REQUIRED:".length());
        log.error(
            "[Gmail API] Autorização OAuth ausente/expirada apos {}ms. Acesse esta URL (logado"
                + " como {}) para autorizar o envio de emails — é a mesma autorização usada pelo"
                + " backup do Google Drive: {}",
            elapsed,
            maskEmail(senderAddress),
            authUrl);
        throw new NotificationException("Autorização do Gmail necessária: " + authUrl, e);
      }
      log.error(
          "[Gmail API] Falha ao preparar credenciais to={} apos {}ms: {}",
          to,
          elapsed,
          e.getMessage(),
          e);
      throw new NotificationException("Falha ao autenticar com o Gmail API: " + e.getMessage(), e);

    } catch (GoogleJsonResponseException e) {
      long elapsed = System.currentTimeMillis() - start;
      String detail = e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage();
      log.error(
          "[Gmail API] A API do Google recusou o envio to={} apos {}ms. httpStatus={} detalhe={}"
              + " (verifique se o escopo gmail.send foi concedido na autorização OAuth)",
          to,
          elapsed,
          e.getStatusCode(),
          detail,
          e);
      throw new NotificationException("Gmail API recusou o envio: " + detail, e);

    } catch (IOException e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error(
          "[Gmail API] Falha de rede/IO ao chamar a API do Google to={} apos {}ms. Como a"
              + " chamada é HTTPS (porta 443), isso normalmente NÃO é bloqueio de porta SMTP —"
              + " verifique conectividade geral de internet do host de deploy. Detalhe: {}",
          to,
          elapsed,
          e.getMessage(),
          e);
      throw new NotificationException("Falha de IO ao enviar via Gmail API: " + e.getMessage(), e);

    } catch (MessagingException e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error(
          "[Gmail API] Erro ao montar a mensagem MIME to={} apos {}ms: {}",
          to,
          elapsed,
          e.getMessage(),
          e);
      throw new NotificationException(
          "Falha ao montar email para Gmail API: " + e.getMessage(), e);
    }
  }

  private Gmail buildGmailClient() {
    try {
      CredentialConfig config =
          CredentialConfig.builder()
              .applicationName(APPLICATION_NAME)
              .tokensDirectoryPath(tokensDirectoryPath)
              .redirectUri(redirectUri)
              .credentialsFile(base64FileDecoder.getGoogleDriveCredentialsFile())
              .build();

      NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      Credential credential = credentialManager.getCredentials(httpTransport, config);

      if (credential.getAccessToken() != null
          && credential.getAccessToken().startsWith("AUTHORIZATION_REQUIRED:")) {
        throw new BackupException(credential.getAccessToken());
      }

      return new Gmail.Builder(httpTransport, GsonFactory.getDefaultInstance(), credential)
          .setApplicationName(APPLICATION_NAME)
          .build();
    } catch (BackupException e) {
      throw e;
    } catch (GeneralSecurityException | IOException e) {
      throw new BackupException("Erro ao criar o cliente do Gmail API.", e);
    }
  }

  private MimeMessage buildMimeMessage(
      String to, String subject, String text, List<byte[]> attachments, List<String> fileNames)
      throws MessagingException {
    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage mimeMessage = new MimeMessage(session);
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

    helper.setFrom(senderAddress);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(text, true);

    addInlineLogo(helper);

    if (attachments != null && fileNames != null) {
      for (int i = 0; i < attachments.size() && i < fileNames.size(); i++) {
        helper.addAttachment(
            fileNames.get(i), new ByteArrayResource(attachments.get(i)));
      }
    }

    return mimeMessage;
  }

  private void addInlineLogo(MimeMessageHelper helper) {
    try {
      ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
      if (logoResource.exists()) {
        helper.addInline("logo", logoResource);
      } else {
        log.warn("[Gmail API] Logo inline static/images/logo.png não encontrado no classpath.");
      }
    } catch (MessagingException e) {
      log.warn("[Gmail API] Falha ao anexar logo inline: {}", e.getMessage());
    }
  }

  private Message toGmailMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    mimeMessage.writeTo(buffer);
    String encoded = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
    return new Message().setRaw(encoded);
  }

  private static String maskEmail(String email) {
    int at = email.indexOf('@');
    if (at <= 1) {
      return "***";
    }
    return email.charAt(0) + "***" + email.substring(at);
  }
}
