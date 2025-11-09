package com.hortifruti.sl.hortifruti.service.backup.auth;

import com.google.api.client.auth.oauth2.Credential;
import org.springframework.stereotype.Component;

@Component
public class TokenValidator {

  /** Verifica se a credencial é válida */
  protected boolean isValidCredential(Credential credential) {
    if (credential == null) {
      return false;
    }

    // Verifica se o token de acesso está presente e não expirou
    if (credential.getAccessToken() != null && isTokenNotExpired(credential)) {
      return true;
    }

    // Tenta renovar o token se expirado
    if (credential.getRefreshToken() != null) {
      return tryRefreshToken(credential);
    }

    return false;
  }

  /** Verifica se o token não expirou */
  private boolean isTokenNotExpired(Credential credential) {
    return credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() > 60;
  }

  /** Tenta renovar o token */
  private boolean tryRefreshToken(Credential credential) {
    try {
      if (credential.refreshToken()) {
        return true;
      }
    } catch (Exception e) {
      System.out.println("Falha ao renovar o token: " + e.getMessage());
    }
    return false;
  }
}
