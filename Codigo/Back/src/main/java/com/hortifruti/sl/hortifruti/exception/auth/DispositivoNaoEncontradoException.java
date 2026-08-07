package com.hortifruti.sl.hortifruti.exception.auth;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class DispositivoNaoEncontradoException extends DomainException {
  public DispositivoNaoEncontradoException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getErrorTitle() {
    return "Dispositivo Não Encontrado";
  }
}
