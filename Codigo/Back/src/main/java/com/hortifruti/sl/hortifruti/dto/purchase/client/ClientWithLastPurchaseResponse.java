package com.hortifruti.sl.hortifruti.dto.purchase.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClientWithLastPurchaseResponse(
    Long clientId,
    String clientName,
    LocalDateTime lastPurchaseDate,
    BigDecimal lastPurchaseTotal) {}
