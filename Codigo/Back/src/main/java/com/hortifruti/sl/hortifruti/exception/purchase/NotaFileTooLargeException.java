package com.hortifruti.sl.hortifruti.exception.purchase;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class NotaFileTooLargeException extends DomainException {

  public NotaFileTooLargeException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.CONTENT_TOO_LARGE;
  }

  @Override
  public String getErrorTitle() {
    return "Arquivo Muito Grande";
  }
}
