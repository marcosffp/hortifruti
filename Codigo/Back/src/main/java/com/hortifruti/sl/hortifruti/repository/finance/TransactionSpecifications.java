package com.hortifruti.sl.hortifruti.repository.finance;

import com.hortifruti.sl.hortifruti.model.finance.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

/**
 * Predicados combináveis para {@link TransactionRepository} (já {@code JpaSpecificationExecutor}) —
 * substitui os métodos {@code @Query} que só filtravam por combinações de
 * período/banco/tipo/categoria, cada um exigindo uma nova assinatura no repositório para cada
 * combinação nova.
 */
public final class TransactionSpecifications {

  private TransactionSpecifications() {}

  public static Specification<Transaction> transactionDateBetween(LocalDate start, LocalDate end) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.between(root.get("transactionDate"), start, end);
  }

  public static Specification<Transaction> createdAtBetween(
      LocalDateTime start, LocalDateTime end) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.between(root.get("createdAt"), start, end);
  }

  public static Specification<Transaction> statementBankEquals(Bank bank) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("statement").get("bank"), bank);
  }
}
