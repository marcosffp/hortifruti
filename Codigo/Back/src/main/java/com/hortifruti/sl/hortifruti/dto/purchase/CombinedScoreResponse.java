package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CombinedScoreResponse(
    Long id,
    Long clientId,
    BigDecimal totalValue,
    boolean paid,
    LocalDateTime dueDate,
    LocalDateTime confirmedAt) {}
