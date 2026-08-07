package com.hortifruti.sl.hortifruti.exception.purchase;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CombinedScoreException extends DomainException {
  public CombinedScoreException(String message) {
    super(message);
  }

  public CombinedScoreException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Erro no Agrupamento de Pontuação Combinada";
  }
}
