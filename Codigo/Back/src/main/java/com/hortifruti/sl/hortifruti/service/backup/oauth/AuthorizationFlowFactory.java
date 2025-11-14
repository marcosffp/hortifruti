package com.hortifruti.sl.hortifruti.service.backup.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.BackupException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationFlowFactory {

  private static final String TOKENS_DIRECTORY_PATH = "temp/google/tokens";
  private final Base64FileDecoder base64FileDecoder;

  /** Cria o contexto completo para o fluxo OAuth. */
  protected OAuthFlowContext createFlowContext() {
    try {
      NetHttpTransport httpTransport = createHttpTransport();
      GoogleAuthorizationCodeFlow flow = createAuthorizationFlow(httpTransport);

      return OAuthFlowContext.builder().httpTransport(httpTransport).flow(flow).build();

    } catch (Exception e) {
      throw new BackupException("Erro ao criar contexto do fluxo OAuth.", e);
    }
  }

  /** Cria o transporte HTTP seguro. */
  private NetHttpTransport createHttpTransport() throws Exception {
    return GoogleNetHttpTransport.newTrustedTransport();
  }

  /** Cria o fluxo de autorização do Google. */
  private GoogleAuthorizationCodeFlow createAuthorizationFlow(NetHttpTransport httpTransport)
      throws IOException {

    GoogleClientSecrets clientSecrets = loadClientSecrets();
    ensureTokensDirectoryExists();

    return new GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            GsonFactory.getDefaultInstance(),
            clientSecrets,
            Collections.singletonList(DriveScopes.DRIVE))
        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build();
  }

  /** Carrega os client secrets do arquivo de credenciais. */
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

  /** Garante que o diretório de tokens existe. */
  private void ensureTokensDirectoryExists() {
    java.io.File tokensDir = new java.io.File(TOKENS_DIRECTORY_PATH);
    if (!tokensDir.exists()) {
      tokensDir.mkdirs();
    }
  }
}
