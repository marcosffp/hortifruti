package com.hortifruti.sl.hortifruti.dto.finance;

import com.hortifruti.sl.hortifruti.model.finance.Category;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de uma transação recém-extraída de um extrato bancário (BB/Sicoob), antes de virar {@link
 * com.hortifruti.sl.hortifruti.model.finance.Transaction} — usado só internamente pelos serviços de
 * importação, não é {@code @RequestBody} de nenhum endpoint.
 */
public record NewTransactionData(
    Statement statement,
    String codHistory,
    String history,
    BigDecimal amount,
    Category category,
    TransactionType transactionType,
    String document,
    String sourceAgency,
    String batch,
    LocalDate transactionDate) {}
