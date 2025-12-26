package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;

public record GroupedProductResponse(
    String code, String name, BigDecimal price, BigDecimal quantity, BigDecimal totalValue) {}
