package com.hortifruti.sl.hortifruti.service.googleauth;

import java.io.File;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CredentialConfig {
  private final String applicationName;
  private final String tokensDirectoryPath;
  private final String redirectUri;
  private final File credentialsFile;

  /**
   * Identifica qual fluxo iniciou a autorização (ex.: "backup" ou "notificacoes") — os dois
   * compartilham o mesmo client OAuth/redirect URI, e este valor viaja como o parâmetro OAuth2
   * {@code state} para o callback saber para qual tela redirecionar o usuário de volta.
   */
  private final String authOrigin;
}
