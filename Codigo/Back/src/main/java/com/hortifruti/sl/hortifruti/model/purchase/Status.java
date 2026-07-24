package com.hortifruti.sl.hortifruti.model.purchase;

import com.hortifruti.sl.hortifruti.exception.purchase.PurchaseException;

public enum Status {
  PENDENTE,
  PAGO,
  CANCELADO,
  CANCELADO_BOLETO,
  CANCELADO_NOTA_FISCAL;

  public static Status fromString(String status) {
    if (status == null || status.trim().isEmpty()) {
      throw new PurchaseException("Status inválido");
    }
    return Status.valueOf(status.toUpperCase());
  }
}
