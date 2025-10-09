package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;

public record GroupedProduct(
    String code, String name, BigDecimal price, Integer quantity, BigDecimal totalValue) {}
