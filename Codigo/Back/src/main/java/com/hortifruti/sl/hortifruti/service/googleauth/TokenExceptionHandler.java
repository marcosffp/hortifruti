package com.hortifruti.sl.hortifruti.service.googleauth;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.gmail.GmailScopes;
import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
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

  private final DatabaseDataStoreFactory dataStoreFactory;

  private static final List<String> SCOPES = List.of(DriveScopes.DRIVE, GmailScopes.GMAIL_SEND);

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
   * O refresh token não é mais válido (ex.: revogado pelo usuário) — não há como recuperar
   * silenciosamente. Limpa a credencial persistida e devolve o mesmo sentinel
   * "AUTHORIZATION_REQUIRED:" usado quando nunca houve credencial, para o chamador propagar a URL
   * de reautorização (fluxo via navegador/redirect do servidor, não um fluxo desktop local).
   */
  private Credential handleInvalidGrantError(
      NetHttpTransport httpTransport, CredentialConfig config) throws IOException {
    StoredCredential.getDefaultDataStore(dataStoreFactory).clear();

    GoogleAuthorizationCodeFlow flow = createNewAuthorizationFlow(httpTransport, config);
    String authorizationUrl =
        flow.newAuthorizationUrl()
            .setRedirectUri(config.getRedirectUri())
            .setState(config.getAuthOrigin())
            .build();

    return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
        .setTransport(httpTransport)
        .setJsonFactory(GsonFactory.getDefaultInstance())
        .build()
        .setAccessToken("AUTHORIZATION_REQUIRED:" + authorizationUrl);
  }

  private GoogleAuthorizationCodeFlow createNewAuthorizationFlow(
      NetHttpTransport httpTransport, CredentialConfig config) throws IOException {
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            GsonFactory.getDefaultInstance(),
            new InputStreamReader(
                new FileInputStream(config.getCredentialsFile()), StandardCharsets.UTF_8));

    return new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, GsonFactory.getDefaultInstance(), clientSecrets, SCOPES)
        .setDataStoreFactory(dataStoreFactory)
        .setAccessType("offline")
        .build();
  }
}
