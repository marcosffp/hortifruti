package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CombinedScoreResponse(
    Long id,
    Long clientId,
    BigDecimal totalValue,
    LocalDateTime dueDate,
    LocalDateTime confirmedAt) {}
