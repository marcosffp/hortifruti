package com.hortifruti.sl.hortifruti.dto.purchase;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CombinedScoreRequest(
    @NotNull(message = "O ID do cliente é obrigatório") Long clientId,
    @NotNull(message = "A data de início é obrigatória") LocalDateTime startDate,
    @NotNull(message = "A data de término é obrigatória") LocalDateTime endDate) {}
