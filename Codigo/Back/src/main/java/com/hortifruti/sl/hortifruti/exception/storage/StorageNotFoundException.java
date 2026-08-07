package com.hortifruti.sl.hortifruti.exception.storage;

import org.springframework.http.HttpStatus;

public class StorageNotFoundException extends StorageException {
  public StorageNotFoundException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getErrorTitle() {
    return "Arquivo Não Encontrado";
  }

  @Override
  public boolean logStackTrace() {
    return false;
  }
}
