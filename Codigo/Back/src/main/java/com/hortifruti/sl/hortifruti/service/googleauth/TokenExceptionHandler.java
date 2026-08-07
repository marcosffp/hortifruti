package com.hortifruti.sl.hortifruti.service.googleauth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
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

  private final AuthorizationHandler authorizationHandler;

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

  private Credential handleInvalidGrantError(
      NetHttpTransport httpTransport, CredentialConfig config) throws IOException {
    TokenCleaner.cleanTokenDirectory(config.getTokensDirectoryPath());

    GoogleAuthorizationCodeFlow flow = createNewAuthorizationFlow(httpTransport, config);
    return authorizationHandler.authorizeUser(flow, "user");
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
        .setDataStoreFactory(
            new FileDataStoreFactory(new java.io.File(config.getTokensDirectoryPath())))
        .setAccessType("offline")
        .build();
  }
}
