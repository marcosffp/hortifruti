package com.hortifruti.sl.hortifruti.exception.purchase;

public class CombinedScoreException extends RuntimeException {
  public CombinedScoreException(String message) {
    super(message);
  }

  public CombinedScoreException(String message, Throwable cause) {
    super(message, cause);
  }
}
