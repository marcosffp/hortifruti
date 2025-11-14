package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;

public record UpdateGroupedProduct(String name, BigDecimal price, Integer quantity) {}
