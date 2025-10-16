package com.hortifruti.sl.hortifruti.exception;

public class BilletException extends RuntimeException {
  public BilletException(String message) {
    super(message);
  }

  public BilletException(String message, Throwable cause) {
    super(message, cause);
  }
}
