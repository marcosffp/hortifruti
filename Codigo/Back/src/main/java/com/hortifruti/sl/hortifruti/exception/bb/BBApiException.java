package com.hortifruti.sl.hortifruti.exception.bb;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class BBApiException extends DomainException {
  public BBApiException(String message) {
    super(message);
  }

  public BBApiException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_GATEWAY;
  }

  @Override
  public String getErrorTitle() {
    return "Erro na Integração com o Banco do Brasil";
  }

  @Override
  public boolean isSevere() {
    return true;
  }
}
