package com.hortifruti.sl.hortifruti.exception.auth;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AuthException extends DomainException {

  public AuthException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.UNAUTHORIZED;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Autenticação";
  }
}
