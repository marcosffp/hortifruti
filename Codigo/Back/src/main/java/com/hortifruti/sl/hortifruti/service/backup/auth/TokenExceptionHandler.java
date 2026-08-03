package com.hortifruti.sl.hortifruti.service.backup.auth;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.gmail.GmailScopes;
import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.repository.OAuthCredentialEntryRepository;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenExceptionHandler {

  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final List<String> SCOPES = List.of(DriveScopes.DRIVE, GmailScopes.GMAIL_SEND);

  private final OAuthCredentialEntryRepository repository;

  protected Credential handleTokenException(
      com.google.api.client.auth.oauth2.TokenResponseException e,
      NetHttpTransport httpTransport,
      CredentialConfig config)
      throws IOException {

    if (isInvalidGrantError(e)) {
      return handleInvalidGrantError(httpTransport, config);
    }

    throw new BackupException("Erro de autenticação no Google Drive.", e);
  }

  private boolean isInvalidGrantError(com.google.api.client.auth.oauth2.TokenResponseException e) {
    return e.getDetails() != null && "invalid_grant".equals(e.getDetails().getError());
  }

  /**
   * A credencial guardada não serve mais (refresh token revogado/expirado no lado do Google) —
   * descarta a entrada e devolve um novo link de autorização, no mesmo formato usado em
   * {@link CredentialManager#handleInvalidCredential}, em vez de tentar um fluxo interativo (que
   * nunca funcionaria num servidor sem navegador/terminal).
   */
  private Credential handleInvalidGrantError(
      NetHttpTransport httpTransport, CredentialConfig config) throws IOException {
    repository.deleteAllByDataStoreId(StoredCredential.DEFAULT_DATA_STORE_ID);

    GoogleAuthorizationCodeFlow flow = createNewAuthorizationFlow(httpTransport, config);
    String authorizationUrl =
        flow.newAuthorizationUrl()
            .setRedirectUri(config.getRedirectUri())
            .setState(config.getAuthOrigin())
            .build();

    return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
        .setTransport(httpTransport)
        .setJsonFactory(JSON_FACTORY)
        .build()
        .setAccessToken("AUTHORIZATION_REQUIRED:" + authorizationUrl);
  }

  private GoogleAuthorizationCodeFlow createNewAuthorizationFlow(
      NetHttpTransport httpTransport, CredentialConfig config) throws IOException {
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            JSON_FACTORY,
            new InputStreamReader(
                new FileInputStream(config.getCredentialsFile()), StandardCharsets.UTF_8));

    return new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
        .setAccessType("offline")
        .build();
  }
}
