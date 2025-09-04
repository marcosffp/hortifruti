package com.hortifruti.sl.hortifruti.dto;

import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    @NotBlank String document,
    @NotBlank String history,
    @NotNull Category category,
    @NotNull TransactionType transactionType,
    @NotNull LocalDate transactionDate,
    @NotNull BigDecimal amount,
    @NotBlank String statement) {}
