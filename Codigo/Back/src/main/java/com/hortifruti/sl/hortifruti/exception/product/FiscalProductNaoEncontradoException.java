package com.hortifruti.sl.hortifruti.exception.product;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class FiscalProductNaoEncontradoException extends DomainException {
  public FiscalProductNaoEncontradoException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getErrorTitle() {
    return "Produto Fiscal Não Encontrado";
  }
}
