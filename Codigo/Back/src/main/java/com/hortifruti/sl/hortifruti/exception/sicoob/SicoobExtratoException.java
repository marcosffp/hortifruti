package com.hortifruti.sl.hortifruti.exception.sicoob;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class SicoobExtratoException extends DomainException {
  public SicoobExtratoException(String message) {
    super(message);
  }

  public SicoobExtratoException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_GATEWAY;
  }

  @Override
  public String getErrorTitle() {
    return "Erro ao consultar extrato do Sicoob";
  }

  @Override
  public boolean isSevere() {
    return true;
  }
}
