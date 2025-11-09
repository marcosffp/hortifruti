package com.hortifruti.sl.hortifruti.service.backup.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

  private final OAuthCallbackHandler oauthCallbackHandler;

  /** Lida com o callback de autorização do Google. */
  public void handleOAuth2Callback(String authorizationCode) {
    oauthCallbackHandler.handleAuthorizationCode(authorizationCode);
  }
}
