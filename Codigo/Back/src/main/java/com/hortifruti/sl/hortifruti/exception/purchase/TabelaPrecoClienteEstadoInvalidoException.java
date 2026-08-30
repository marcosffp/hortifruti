package com.hortifruti.sl.hortifruti.exception.purchase;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Cobre as duas guardas de estado da tabela de preços: tentar confirmar com item ainda {@code
 * SUGERIDO} sem decisão humana, e tentar editar uma tabela que já está {@code CONFIRMADA}
 * (confirmada é dado sensível, não se sobrescreve sem rastro — ver reimport versionado em {@code
 * TabelaPrecoClienteImportService}).
 */
public class TabelaPrecoClienteEstadoInvalidoException extends DomainException {
  public TabelaPrecoClienteEstadoInvalidoException(String message) {
    super(message);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getErrorTitle() {
    return "Estado Inválido da Tabela de Preços";
  }
}
