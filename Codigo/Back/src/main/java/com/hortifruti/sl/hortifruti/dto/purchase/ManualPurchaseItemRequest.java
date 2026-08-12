package com.hortifruti.sl.hortifruti.dto.purchase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ManualPurchaseItemRequest(
    @NotBlank(message = "Código do produto é obrigatório") String code,
    @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser positiva")
        BigDecimal quantity,
    @NotNull(message = "Preço é obrigatório") @Positive(message = "Preço deve ser positivo")
        BigDecimal price) {}
