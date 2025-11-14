package com.hortifruti.sl.hortifruti.service.backup.oauth;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

  private final OAuthCallbackHandler oauthCallbackHandler;

  @Value("${frontend.url}")
  private String frontendUrl;

  /** Processa o callback de autorização do Google e gerencia redirecionamentos. */
  public void processOAuth2Callback(String authorizationCode, HttpServletResponse response) {
    try {
      oauthCallbackHandler.handleAuthorizationCode(authorizationCode);
      String redirectUrl = frontendUrl + "/backup?auth=success";
      response.sendRedirect(redirectUrl);
    } catch (Exception e) {
      try {
        String redirectUrl =
            frontendUrl
                + "/backup?auth=error&message="
                + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
      } catch (IOException ioException) {
        throw new BackupException(
            "Erro no redirecionamento após falha na autenticação OAuth.", ioException);
      }
    }
  }
}
