package com.hortifruti.sl.hortifruti.exception.purchase;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class TabelaPrecoClienteNaoEncontradaException extends DomainException {
  public TabelaPrecoClienteNaoEncontradaException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getErrorTitle() {
    return "Tabela de Preços Não Encontrada";
  }
}
