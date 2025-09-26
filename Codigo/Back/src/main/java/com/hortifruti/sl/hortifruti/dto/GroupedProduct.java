package com.hortifruti.sl.hortifruti.dto;

import java.math.BigDecimal;

public record GroupedProduct(
    String code, String name, BigDecimal price, Integer quantity, BigDecimal totalValue) {}
