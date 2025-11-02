package com.hortifruti.sl.hortifruti.service.backup.auth;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.hortifruti.sl.hortifruti.exception.BackupException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CredentialManager {

  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
  private final TokenValidator tokenValidator;
  private final TokenExceptionHandler tokenExceptionHandler;

  /** Obtém credenciais válidas para o Google Drive */
  public Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT, CredentialConfig config)
      throws IOException {

    GoogleAuthorizationCodeFlow flow = createAuthorizationFlow(HTTP_TRANSPORT, config);
    return validateAndGetCredential(HTTP_TRANSPORT, config, flow);
  }

  /** Cria o fluxo de autorização do Google */
  private GoogleAuthorizationCodeFlow createAuthorizationFlow(
      NetHttpTransport httpTransport, CredentialConfig config) throws IOException {
    GoogleClientSecrets clientSecrets = loadClientSecrets(config.getCredentialsFile());

    return new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
        .setDataStoreFactory(
            new FileDataStoreFactory(new java.io.File(config.getTokensDirectoryPath())))
        .setAccessType("offline")
        .build();
  }

  /** Valida e obtém a credencial */
  private Credential validateAndGetCredential(
      NetHttpTransport httpTransport, CredentialConfig config, GoogleAuthorizationCodeFlow flow)
      throws IOException {
    try {
      Credential credential = flow.loadCredential("user");

      if (tokenValidator.isValidCredential(credential)) {
        return credential;
      }

      return handleInvalidCredential(httpTransport, config, flow);

    } catch (com.google.api.client.auth.oauth2.TokenResponseException e) {
      return tokenExceptionHandler.handleTokenException(e, httpTransport, config);
    }
  }

  /** Manipula credenciais inválidas */
  private Credential handleInvalidCredential(
      NetHttpTransport httpTransport, CredentialConfig config, GoogleAuthorizationCodeFlow flow)
      throws IOException {

    String authorizationUrl =
        flow.newAuthorizationUrl().setRedirectUri(config.getRedirectUri()).build();

    // Retornar uma credencial fictícia com o link de autorização embutido
    return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
        .setTransport(httpTransport)
        .setJsonFactory(JSON_FACTORY)
        .build()
        .setAccessToken("AUTHORIZATION_REQUIRED:" + authorizationUrl);
  }

  /** Carrega os client secrets do arquivo de credenciais */
  private GoogleClientSecrets loadClientSecrets(java.io.File credentialsFile) throws IOException {
    if (!credentialsFile.exists()) {
      throw new BackupException("Arquivo de credenciais não encontrado.");
    }

    return GoogleClientSecrets.load(
        JSON_FACTORY,
        new InputStreamReader(new FileInputStream(credentialsFile), StandardCharsets.UTF_8));
  }
}
