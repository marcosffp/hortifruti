package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;
import java.util.List;

public record GroupedProductsResponse(
    String clientName, int totalItems, BigDecimal totalValue, List<GroupedProduct> products) {}
