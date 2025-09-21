package com.hortifruti.sl.hortifruti.dto.transaction;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
    Long id,
    String document,
    String history,
    Category category,
    TransactionType transactionType,
    LocalDate transactionDate,
    BigDecimal amount,
    Bank bank) {}
