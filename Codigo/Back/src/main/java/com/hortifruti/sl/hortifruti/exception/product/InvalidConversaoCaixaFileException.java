package com.hortifruti.sl.hortifruti.exception.product;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidConversaoCaixaFileException extends DomainException {

  public InvalidConversaoCaixaFileException(String message) {
    super(message);
  }

  public InvalidConversaoCaixaFileException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Arquivo de Conversão Inválido";
  }
}
