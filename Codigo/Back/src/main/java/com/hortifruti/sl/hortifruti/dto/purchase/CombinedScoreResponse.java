package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CombinedScoreResponse(
    Long id,
    Long clientId,
    BigDecimal totalValue,
    boolean paid,
    LocalDate dueDate,
    LocalDate confirmedAt) {}
