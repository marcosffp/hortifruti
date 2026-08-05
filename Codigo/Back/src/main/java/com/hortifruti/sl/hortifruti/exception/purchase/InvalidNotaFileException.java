package com.hortifruti.sl.hortifruti.exception.purchase;

public class InvalidNotaFileException extends RuntimeException {

  public InvalidNotaFileException(String message) {
    super(message);
  }

  public InvalidNotaFileException(String message, Throwable cause) {
    super(message, cause);
  }
}
