package com.hortifruti.sl.hortifruti.exception.auth;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class DispositivoException extends DomainException {
  public DispositivoException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Pareamento";
  }
}
