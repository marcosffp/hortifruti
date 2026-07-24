package com.hortifruti.sl.hortifruti.exception.invoice;

/**
 * Sinaliza que a Focus NFe recusou o cancelamento porque a NF-e já estava cancelada (código {@code
 * already_processed}). Esse estado é o resultado desejado da operação de cancelar, então deve ser
 * tratado como sucesso/no-op pelo chamador, e não propagado como falha.
 */
public class InvoiceAlreadyCancelledException extends InvoiceException {
  public InvoiceAlreadyCancelledException(String message, Throwable cause) {
    super(message, cause);
  }
}
