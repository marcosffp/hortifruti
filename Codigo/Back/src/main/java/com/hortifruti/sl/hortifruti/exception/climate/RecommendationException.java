package com.hortifruti.sl.hortifruti.exception.climate;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RecommendationException extends DomainException {
  public RecommendationException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Recomendação";
  }
}
