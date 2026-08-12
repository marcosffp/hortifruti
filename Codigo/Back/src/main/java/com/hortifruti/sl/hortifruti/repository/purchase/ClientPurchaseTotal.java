package com.hortifruti.sl.hortifruti.repository.purchase;

import java.math.BigDecimal;

/**
 * Projeção de {@link PurchaseRepository#sumTotalGroupedByClientId()} — soma de compras por cliente.
 */
public record ClientPurchaseTotal(Long clientId, BigDecimal total) {}
