package com.hortifruti.sl.hortifruti.dto.finance;

import com.hortifruti.sl.hortifruti.model.finance.Category;
import com.hortifruti.sl.hortifruti.model.finance.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    @NotNull LocalDate transactionDate,
    @NotBlank String codHistory,
    @NotBlank String history,
    @NotNull BigDecimal amount,
    @NotNull Category category,
    @NotNull TransactionType transactionType,
    @NotBlank String document,
    @NotBlank String sourceAgency,
    @NotBlank String batch) {}
