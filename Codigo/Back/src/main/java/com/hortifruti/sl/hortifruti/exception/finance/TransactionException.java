package com.hortifruti.sl.hortifruti.exception.finance;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class TransactionException extends DomainException {
  public TransactionException(String message) {
    super(message);
  }

  public TransactionException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Transação";
  }
}
