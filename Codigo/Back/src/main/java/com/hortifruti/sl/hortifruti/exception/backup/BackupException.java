package com.hortifruti.sl.hortifruti.exception.backup;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class BackupException extends DomainException {
  public BackupException(String message) {
    super(message);
  }

  public BackupException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Backup";
  }

  @Override
  public boolean logStackTrace() {
    return true;
  }
}
