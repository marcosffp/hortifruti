package com.hortifruti.sl.hortifruti.dto.purchase;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record WildcardBilletRequest(
    @NotNull(message = "O ID do cliente é obrigatório") Long clientId,
    @NotNull(message = "O valor é obrigatório") @Positive(message = "O valor deve ser maior que zero")
        BigDecimal value) {}
