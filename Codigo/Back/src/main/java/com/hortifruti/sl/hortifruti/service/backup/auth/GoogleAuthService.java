package com.hortifruti.sl.hortifruti.service.backup.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.BackupException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

  private static final String APPLICATION_NAME = "Hortifruti SL Backup";
  private final Base64FileDecoder base64FileDecoder;
  private final CredentialManager credentialManager;

  @Value("${google.redirect.uri}")
  private String redirectUri;

  @Value("${google.tokens.directory}")
  private String tokensDirectoryPath;

  /** Cria um cliente autorizado do Google Drive. */
  public Drive getDriveService() {
    try {
      CredentialConfig config =
          CredentialConfig.builder()
              .applicationName(APPLICATION_NAME)
              .tokensDirectoryPath(tokensDirectoryPath)
              .redirectUri(redirectUri)
              .credentialsFile(base64FileDecoder.getGoogleDriveCredentialsFile())
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

      return drive;
    } catch (BackupException e) {
      throw e;
    } catch (Exception e) {
      throw new BackupException("Erro ao criar o cliente do Google Drive.", e);
    }
  }
}
