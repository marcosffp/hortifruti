package com.hortifruti.sl.hortifruti.service.backup.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationHandler {

  /** Autoriza o usuário via fluxo OAuth2 */
  protected Credential authorizeUser(GoogleAuthorizationCodeFlow flow, String userId)
      throws IOException {

    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

    try {
      Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize(userId);
      return credential;
    } finally {
      try {
        receiver.stop();
      } catch (IOException e) {
        System.out.println("Erro ao parar o servidor local do receptor: " + e.getMessage());
      }
    }
  }
}
