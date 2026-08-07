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

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.FORBIDDEN;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Token";
  }
}
