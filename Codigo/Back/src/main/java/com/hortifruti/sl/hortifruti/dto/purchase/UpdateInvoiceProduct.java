package com.hortifruti.sl.hortifruti.dto.purchase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateInvoiceProduct(
    @NotBlank(message = "Código do produto é obrigatório") String code,
    @NotBlank(message = "Nome do produto é obrigatório") String name,
    @NotNull(message = "Preço é obrigatório") @Positive(message = "Preço deve ser positivo")
        BigDecimal price,
    @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser positiva")
        BigDecimal quantity,
    @NotBlank(message = "Tipo de unidade é obrigatório") String unitType) {}
