package com.hortifruti.sl.hortifruti.exception;

public class PurchaseProcessingException extends RuntimeException {

  public PurchaseProcessingException(String message) {
    super(message);
  }

  public PurchaseProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
