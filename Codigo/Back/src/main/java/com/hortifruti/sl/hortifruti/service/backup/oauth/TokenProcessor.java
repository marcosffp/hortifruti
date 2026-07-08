package com.hortifruti.sl.hortifruti.service.backup.oauth;

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

  protected void processAuthorizationCode(String authorizationCode, OAuthFlowContext context)
      throws IOException {

    TokenResponse tokenResponse = exchangeCodeForToken(authorizationCode, context.getFlow());
    storeCredentials(tokenResponse, context.getFlow());
  }

  private TokenResponse exchangeCodeForToken(
      String authorizationCode, GoogleAuthorizationCodeFlow flow) throws IOException {

    return flow.newTokenRequest(authorizationCode).setRedirectUri(redirectUri).execute();
  }

  private void storeCredentials(TokenResponse tokenResponse, GoogleAuthorizationCodeFlow flow)
      throws IOException {

    flow.createAndStoreCredential(tokenResponse, "user");
  }
}
