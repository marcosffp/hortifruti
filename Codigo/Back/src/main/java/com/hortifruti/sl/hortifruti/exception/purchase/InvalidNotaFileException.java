package com.hortifruti.sl.hortifruti.exception.purchase;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidNotaFileException extends DomainException {

  public InvalidNotaFileException(String message) {
    super(message);
  }

  public InvalidNotaFileException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Arquivo Inválido";
  }
}
