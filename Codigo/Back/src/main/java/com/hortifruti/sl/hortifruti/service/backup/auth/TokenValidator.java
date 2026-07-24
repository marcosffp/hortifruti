package com.hortifruti.sl.hortifruti.service.backup.auth;

import com.google.api.client.auth.oauth2.Credential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenValidator {

  protected boolean isValidCredential(Credential credential) {
    if (credential == null) {
      return false;
    }

    if (credential.getAccessToken() != null && isTokenNotExpired(credential)) {
      return true;
    }

    if (credential.getRefreshToken() != null) {
      return tryRefreshToken(credential);
    }

    return false;
  }

  private boolean isTokenNotExpired(Credential credential) {
    return credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() > 60;
  }

  private boolean tryRefreshToken(Credential credential) {
    try {
      if (credential.refreshToken()) {
        return true;
      }
    } catch (Exception e) {
      log.warn("Falha ao renovar o token: {}", e.getMessage(), e);
    }
    return false;
  }
}
