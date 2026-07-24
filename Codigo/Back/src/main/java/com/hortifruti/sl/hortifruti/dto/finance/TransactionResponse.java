package com.hortifruti.sl.hortifruti.dto.finance;

import com.hortifruti.sl.hortifruti.model.finance.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Category;
import com.hortifruti.sl.hortifruti.model.finance.StatementOrigin;
import com.hortifruti.sl.hortifruti.model.finance.TransactionType;
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
    Bank bank,
    StatementOrigin origin) {}
