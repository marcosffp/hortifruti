package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;

public record InvoiceProductResponse(
    Long id, String code, String name, BigDecimal price, Integer quantity, String unitType) {}
