package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseImageResponse(
    Long purchaseId, LocalDateTime purchaseDate, BigDecimal total) {}
