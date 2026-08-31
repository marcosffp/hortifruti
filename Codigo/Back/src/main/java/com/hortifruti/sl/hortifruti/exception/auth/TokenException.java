package com.hortifruti.sl.hortifruti.exception.auth;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class TokenException extends DomainException {
  public TokenException(String message) {
    super(message);
  }

  public TokenException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 401, não 403: token ausente/inválido/expirado/revogado é problema de autenticação, não de
   * autorização — precisa ficar distinto de {@code AccessDeniedException} (role incorreta, 403) e
   * do bloqueio de origem forjada em {@code SecurityFilter} (também 403), para que um interceptor
   * HTTP no cliente consiga decidir "tento /auth/refresh" (401) vs. "não adianta tentar de novo,
   * é erro de permissão" (403) só olhando o status, sem precisar inspecionar o corpo da resposta.
   */
  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.UNAUTHORIZED;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Token";
  }
}
