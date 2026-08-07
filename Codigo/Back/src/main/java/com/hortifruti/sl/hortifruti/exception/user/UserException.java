package com.hortifruti.sl.hortifruti.exception.user;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class UserException extends DomainException {
  public UserException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Usuário";
  }
}
