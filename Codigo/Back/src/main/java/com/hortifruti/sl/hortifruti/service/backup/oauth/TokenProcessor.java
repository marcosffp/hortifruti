package com.hortifruti.sl.hortifruti.service.backup.oauth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenProcessor {

  @Value("${google.redirect.uri}")
  private String redirectUri;

  /** Processa o código de autorização e armazena as credenciais. */
  protected void processAuthorizationCode(String authorizationCode, OAuthFlowContext context)
      throws IOException {

    TokenResponse tokenResponse = exchangeCodeForToken(authorizationCode, context.getFlow());
    Credential credential = storeCredentials(tokenResponse, context.getFlow());

    logTokenInfo(credential);
  }

  /** Troca o código de autorização por tokens de acesso. */
  private TokenResponse exchangeCodeForToken(
      String authorizationCode, GoogleAuthorizationCodeFlow flow) throws IOException {

    return flow.newTokenRequest(authorizationCode).setRedirectUri(redirectUri).execute();
  }

  /** Armazena as credenciais obtidas. */
  private Credential storeCredentials(TokenResponse tokenResponse, GoogleAuthorizationCodeFlow flow)
      throws IOException {

    return flow.createAndStoreCredential(tokenResponse, "user");
  }

  /** Loga informações do token para debugging. */
  private void logTokenInfo(Credential credential) {
    System.out.println("Informações do Token:");
    System.out.println("Token de autorização processado e armazenado com sucesso.");
    System.out.println(
        "Access Token: " + (credential.getAccessToken() != null ? "Presente" : "Ausente"));
    System.out.println(
        "Refresh Token: " + (credential.getRefreshToken() != null ? "Presente" : "Ausente"));

    if (credential.getExpiresInSeconds() != null) {
      System.out.println("Token expira em: " + credential.getExpiresInSeconds() + " segundos");
    }
  }
}
