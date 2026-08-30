package com.hortifruti.sl.hortifruti.exception.purchase;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidTabelaPrecoClienteFileException extends DomainException {

  public InvalidTabelaPrecoClienteFileException(String message) {
    super(message);
  }

  public InvalidTabelaPrecoClienteFileException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Arquivo de Tabela de Preços Inválido";
  }
}
