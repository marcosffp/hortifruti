package com.hortifruti.sl.hortifruti.service.backup.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.gmail.GmailScopes;
import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.service.backup.auth.DatabaseDataStoreFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationFlowFactory {

  private final Base64FileDecoder base64FileDecoder;
  private final DatabaseDataStoreFactory databaseDataStoreFactory;

  protected OAuthFlowContext createFlowContext() {
    try {
      NetHttpTransport httpTransport = createHttpTransport();
      GoogleAuthorizationCodeFlow flow = createAuthorizationFlow(httpTransport);

      return OAuthFlowContext.builder().httpTransport(httpTransport).flow(flow).build();

    } catch (Exception e) {
      throw new BackupException("Erro ao criar contexto do fluxo OAuth.", e);
    }
  }

  private NetHttpTransport createHttpTransport() throws Exception {
    return GoogleNetHttpTransport.newTrustedTransport();
  }

  private GoogleAuthorizationCodeFlow createAuthorizationFlow(NetHttpTransport httpTransport)
      throws IOException {

    GoogleClientSecrets clientSecrets = loadClientSecrets();

    return new GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            GsonFactory.getDefaultInstance(),
            clientSecrets,
            List.of(DriveScopes.DRIVE, GmailScopes.GMAIL_SEND))
        .setDataStoreFactory(databaseDataStoreFactory)
        .setAccessType("offline")
        .build();
  }

  private GoogleClientSecrets loadClientSecrets() throws IOException {

    base64FileDecoder.decodeGoogleDriveCredentials();
    java.io.File credentialsFile = base64FileDecoder.getGoogleDriveCredentialsFile();

    if (!credentialsFile.exists()) {
      throw new BackupException(
          "Arquivo de credenciais não encontrado: " + credentialsFile.getAbsolutePath());
    }

    return GoogleClientSecrets.load(
        GsonFactory.getDefaultInstance(),
        new InputStreamReader(new FileInputStream(credentialsFile), StandardCharsets.UTF_8));
  }
}
