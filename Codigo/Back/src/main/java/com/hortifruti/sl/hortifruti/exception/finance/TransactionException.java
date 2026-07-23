package com.hortifruti.sl.hortifruti.exception;

public class TransactionException extends RuntimeException {
  public TransactionException(String message) {
    super(message);
  }

  public TransactionException(String message, Throwable cause) {
    super(message, cause);
  }
}
