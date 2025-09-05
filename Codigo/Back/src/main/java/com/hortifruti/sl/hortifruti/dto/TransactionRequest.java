package com.hortifruti.sl.hortifruti.dto;

import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    @NotBlank String statement,
    @NotNull LocalDate transactionDate,
    @NotBlank String codHistory, // Corrigido o nome do campo
    @NotBlank String history,
    @NotNull BigDecimal amount,
    @NotNull Category category,
    @NotNull TransactionType transactionType,
    @NotBlank String document,
    @NotBlank String sourceAgency,
    @NotBlank String batch) {}
