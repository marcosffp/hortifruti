package com.hortifruti.sl.hortifruti.exception.auth;

public class AccountLockedException extends AuthException {

  private final long retryAfterSeconds;

  public AccountLockedException(String message, long retryAfterSeconds) {
    super(message);
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
