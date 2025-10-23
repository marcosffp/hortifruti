package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;

public record UpdateInvoiceProduct(
    String code, String name, BigDecimal price, Integer quantity, String unitType) {}
