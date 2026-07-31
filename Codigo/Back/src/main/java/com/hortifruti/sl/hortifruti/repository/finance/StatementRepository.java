package com.hortifruti.sl.hortifruti.repository.finance;

import com.hortifruti.sl.hortifruti.model.finance.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.StatementOrigin;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StatementRepository extends JpaRepository<Statement, Long> {

  List<Statement> findByBankAndCreatedAtBetween(
      Bank bank, LocalDateTime startDate, LocalDateTime endDate);

  Optional<Statement> findTopByBankOrderByCreatedAtDesc(Bank bank);

  /**
   * Statement "atual" de um banco pra um dado origin (ex.: o último extrato buscado via API) —
   * usado pra decidir se um período pedido de novo já foi processado, e pra sobrescrever esse
   * registro em vez de criar um novo a cada busca.
   */
  Optional<Statement> findTopByBankAndOriginOrderByCreatedAtDesc(Bank bank, StatementOrigin origin);

  @Query(
      "SELECT DISTINCT s FROM Statement s JOIN s.transactions t "
          + "WHERE s.bank = :bank AND t.transactionDate BETWEEN :startDate AND :endDate "
          + "ORDER BY s.createdAt DESC")
  List<Statement> findStatementsWithTransactionsInPeriod(
      @Param("bank") Bank bank,
      @Param("startDate") java.time.LocalDate startDate,
      @Param("endDate") java.time.LocalDate endDate);

  @Query(
      "SELECT s FROM Statement s JOIN s.transactions t "
          + "WHERE s.bank = :bank AND t.transactionDate BETWEEN :startDate AND :endDate "
          + "GROUP BY s.id "
          + "ORDER BY COUNT(t.id) DESC")
  List<Statement> findBestCoverageStatementsForPeriod(
      @Param("bank") Bank bank,
      @Param("startDate") java.time.LocalDate startDate,
      @Param("endDate") java.time.LocalDate endDate);

  List<Statement> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  @Query(
      """
        SELECT s FROM Statement s
        LEFT JOIN FETCH s.transactions
        WHERE s.createdAt BETWEEN :startDate AND :endDate
    """)
  List<Statement> findByCreatedAtBetweenWithTransactions(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
