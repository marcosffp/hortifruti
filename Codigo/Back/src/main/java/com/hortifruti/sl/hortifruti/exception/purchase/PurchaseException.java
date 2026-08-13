package com.hortifruti.sl.hortifruti.exception.purchase;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PurchaseException extends DomainException {

  private final boolean unexpected;

  public PurchaseException(String message) {
    super(message);
    this.unexpected = false;
  }

  public PurchaseException(String message, Throwable cause) {
    super(message, cause);
    this.unexpected = false;
  }

  /**
   * Use quando a causa não é uma falha de validação/formato já prevista (ex.: PDF de layout
   * inesperado), e sim um erro não antecipado no próprio parser — {@code unexpected=true} loga com
   * stacktrace completo para diferenciar "fornecedor mudou o formato do PDF" de "bug no parser",
   * que hoje chegam ao mesmo status HTTP e mensagem genérica e são indistinguíveis em produção.
   */
  public PurchaseException(String message, Throwable cause, boolean unexpected) {
    super(message, cause);
    this.unexpected = unexpected;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Erro no Processamento da Compra";
  }

  @Override
  public boolean logStackTrace() {
    return unexpected;
  }
}
