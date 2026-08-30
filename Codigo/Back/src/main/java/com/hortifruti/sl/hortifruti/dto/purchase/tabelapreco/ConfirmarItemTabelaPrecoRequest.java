package com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco;

/**
 * {@code fiscalProductCode} (não {@code fiscalProductId}) pra bater com a convenção já usada no
 * resto do frontend pra referenciar um {@code FiscalProduct} (ex.: {@code
 * ManualPurchaseItemRequest.code}, {@code ProductAutocompleteField}) — o catálogo exposto ao front
 * (`GET /fiscal-products`) não inclui {@code id}.
 */
public record ConfirmarItemTabelaPrecoRequest(String fiscalProductCode) {}
