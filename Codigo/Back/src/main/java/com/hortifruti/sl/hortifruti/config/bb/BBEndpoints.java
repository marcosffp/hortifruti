package com.hortifruti.sl.hortifruti.config.bb;

import com.hortifruti.sl.hortifruti.exception.bb.BBApiException;

/** Resolve as URLs da API do BB (OAuth2 e Extratos v2) a partir do ambiente configurado. */
public final class BBEndpoints {

  private BBEndpoints() {}

  public static String oauthTokenUrl(String ambiente) {
    return switch (normalize(ambiente)) {
      case "hml" -> "https://oauth.hm.bb.com.br/oauth/token";
      case "prod" -> "https://oauth.bb.com.br/oauth/token";
      default -> throw invalidAmbiente(ambiente);
    };
  }

  public static String extratoBaseUrl(String ambiente) {
    return switch (normalize(ambiente)) {
      case "hml" -> "https://extratos.mtls.api.hm.bb.com.br/v2";
      case "prod" -> "https://extratos.mtls.api.bb.com.br/v2";
      default -> throw invalidAmbiente(ambiente);
    };
  }

  private static String normalize(String ambiente) {
    return ambiente == null ? "" : ambiente.strip().toLowerCase();
  }

  private static BBApiException invalidAmbiente(String ambiente) {
    return new BBApiException("bb.ambiente inválido: '" + ambiente + "' (use 'hml' ou 'prod').");
  }
}
