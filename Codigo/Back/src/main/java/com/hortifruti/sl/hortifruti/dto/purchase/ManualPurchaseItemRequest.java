package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;

public record ManualPurchaseItemRequest(String code, BigDecimal quantity, BigDecimal price) {}
