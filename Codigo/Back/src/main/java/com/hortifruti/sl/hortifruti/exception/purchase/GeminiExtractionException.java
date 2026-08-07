package com.hortifruti.sl.hortifruti.exception.purchase;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class GeminiExtractionException extends DomainException {

  public GeminiExtractionException(String message) {
    super(message);
  }

  public GeminiExtractionException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_GATEWAY;
  }

  @Override
  public String getErrorTitle() {
    return "Erro na Extração da Nota";
  }

  @Override
  public boolean isSevere() {
    return true;
  }
}
