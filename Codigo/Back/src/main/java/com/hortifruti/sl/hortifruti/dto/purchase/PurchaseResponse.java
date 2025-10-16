package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseResponse(
    Long id, LocalDateTime purchaseDate, BigDecimal total, LocalDateTime updatedAt) {}
