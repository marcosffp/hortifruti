package com.hortifruti.sl.hortifruti.service.notification.email;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.exception.notification.NotificationException;
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
import lombok.extern.slf4j.Slf4j;
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
 * REST do Google, então não é afetado por bloqueios de porta SMTP comuns em hosts de deploy.
 */
@Slf4j
@Component
public class GmailApiEmailSender implements EmailSender {

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
    try {
      // Verifica a autorização OAuth antes do endereço remetente: se a conta Google nunca
      // foi autorizada, é mais útil o usuário ver o link de autorização (acionável) do que o
      // erro de configuração abaixo, que exige acesso ao servidor para corrigir.
      Gmail gmail = buildGmailClient();

      if (senderAddress == null || senderAddress.isBlank()) {
        throw new NotificationException("Conta do Gmail não configurada (GMAIL)");
      }

      MimeMessage mimeMessage = buildMimeMessage(to, subject, text, attachments, fileNames);
      Message message = toGmailMessage(mimeMessage);

      gmail.users().messages().send("me", message).execute();
      return true;

    } catch (BackupException e) {
      if (e.getMessage() != null && e.getMessage().startsWith("AUTHORIZATION_REQUIRED:")) {
        String authUrl = e.getMessage().substring("AUTHORIZATION_REQUIRED:".length());
        throw new NotificationException("Autorização do Gmail necessária: " + authUrl, e);
      }
      log.error("Falha ao autenticar com o Gmail API: {}", e.getMessage());
      throw new NotificationException("Falha ao autenticar com o Gmail API: " + e.getMessage(), e);

    } catch (GoogleJsonResponseException e) {
      String detail = e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage();
      log.error("Gmail API recusou o envio para {}: {}", to, detail);
      throw new NotificationException("Gmail API recusou o envio: " + detail, e);

    } catch (IOException e) {
      log.error("Falha de IO ao enviar email via Gmail API para {}: {}", to, e.getMessage());
      throw new NotificationException("Falha de IO ao enviar via Gmail API: " + e.getMessage(), e);

    } catch (MessagingException e) {
      log.error("Falha ao montar email para Gmail API: {}", e.getMessage());
      throw new NotificationException("Falha ao montar email para Gmail API: " + e.getMessage(), e);
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
        helper.addAttachment(fileNames.get(i), new ByteArrayResource(attachments.get(i)));
      }
    }

    return mimeMessage;
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

  private Message toGmailMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    mimeMessage.writeTo(buffer);
    String encoded = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
    return new Message().setRaw(encoded);
  }
}
