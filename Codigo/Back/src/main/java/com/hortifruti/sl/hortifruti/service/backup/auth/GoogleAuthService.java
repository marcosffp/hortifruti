package com.hortifruti.sl.hortifruti.service.backup.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.service.googleauth.CredentialConfig;
import com.hortifruti.sl.hortifruti.service.googleauth.CredentialManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

  private static final String APPLICATION_NAME = "Hortifruti SL Backup";

  /**
   * Mesma margem usada por {@code TokenValidator} para considerar um access token "perto de
   * expirar" — evita reconstruir o cliente Drive (com handshake OAuth) a cada chamada dentro da
   * mesma operação de backup, que faz várias chamadas em sequência.
   */
  private static final long MIN_VALID_SECONDS = 60;

  private final Base64FileDecoder base64FileDecoder;
  private final CredentialManager credentialManager;

  @Value("${google.redirect.uri}")
  private String redirectUri;

  private volatile Credential cachedCredential;
  private volatile Drive cachedDrive;

  public synchronized Drive getDriveService() {
    if (cachedDrive != null && isStillValid(cachedCredential)) {
      return cachedDrive;
    }

    try {
      CredentialConfig config =
          CredentialConfig.builder()
              .applicationName(APPLICATION_NAME)
              .redirectUri(redirectUri)
              .credentialsFile(base64FileDecoder.getGoogleDriveCredentialsFile())
              .authOrigin("backup")
              .build();

      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      Credential credential = credentialManager.getCredentials(HTTP_TRANSPORT, config);

      if (credential.getAccessToken() != null
          && credential.getAccessToken().startsWith("AUTHORIZATION_REQUIRED:")) {
        String authUrl = credential.getAccessToken().substring("AUTHORIZATION_REQUIRED:".length());
        throw new BackupException("AUTHORIZATION_REQUIRED:" + authUrl);
      }

      Drive drive =
          new Drive.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), credential)
              .setApplicationName(APPLICATION_NAME)
              .build();

      cachedCredential = credential;
      cachedDrive = drive;

      return drive;
    } catch (BackupException e) {
      throw e;
    } catch (Exception e) {
      throw new BackupException("Erro ao criar o cliente do Google Drive.", e);
    }
  }

  private boolean isStillValid(Credential credential) {
    return credential != null
        && credential.getExpiresInSeconds() != null
        && credential.getExpiresInSeconds() > MIN_VALID_SECONDS;
  }
}
