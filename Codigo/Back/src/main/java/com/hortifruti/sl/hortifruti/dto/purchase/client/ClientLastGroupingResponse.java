package com.hortifruti.sl.hortifruti.dto.purchase.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClientLastGroupingResponse(
    Long clientId, LocalDate confirmedAt, BigDecimal totalValue) {}
