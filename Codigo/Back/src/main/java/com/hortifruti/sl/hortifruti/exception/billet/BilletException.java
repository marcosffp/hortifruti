package com.hortifruti.sl.hortifruti.exception.billet;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class BilletException extends DomainException {
  public BilletException(String message) {
    super(message);
  }

  public BilletException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Erro na Integração com Sicoob";
  }

  @Override
  public boolean isSevere() {
    return true;
  }
}
