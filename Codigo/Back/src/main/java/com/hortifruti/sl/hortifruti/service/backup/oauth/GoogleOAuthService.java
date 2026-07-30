package com.hortifruti.sl.hortifruti.service.backup.oauth;

import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
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

  /**
   * @param state Origem da autorização ("notificacoes" ou "backup"), propagada pelo Google a partir
   *     do valor definido em {@code CredentialConfig#getAuthOrigin()} ao gerar a URL de
   *     autorização. Qualquer valor diferente de "notificacoes" volta para "/backup" (padrão
   *     anterior a essa distinção existir).
   */
  public void processOAuth2Callback(
      String authorizationCode, String state, HttpServletResponse response) {
    String redirectPath = "notificacoes".equals(state) ? "/notificacoes" : "/backup";
    try {
      oauthCallbackHandler.handleAuthorizationCode(authorizationCode);
      String redirectUrl = frontendUrl + redirectPath + "?auth=success";
      response.sendRedirect(redirectUrl);
    } catch (Exception e) {
      try {
        String redirectUrl =
            frontendUrl
                + redirectPath
                + "?auth=error&message="
                + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
      } catch (IOException ioException) {
        throw new BackupException(
            "Erro no redirecionamento após falha na autenticação OAuth.", ioException);
      }
    }
  }
}
