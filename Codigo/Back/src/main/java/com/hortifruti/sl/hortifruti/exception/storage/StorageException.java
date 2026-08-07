package com.hortifruti.sl.hortifruti.exception.storage;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class StorageException extends DomainException {
  public StorageException(String message) {
    super(message);
  }

  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Armazenamento";
  }

  @Override
  public boolean logStackTrace() {
    return true;
  }
}
