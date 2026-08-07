package com.hortifruti.sl.hortifruti.exception.invoice;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvoiceException extends DomainException {
  public InvoiceException(String message) {
    super(message);
  }

  public InvoiceException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Fatura";
  }
}
