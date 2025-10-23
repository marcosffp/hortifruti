package com.hortifruti.sl.hortifruti.repository.finance;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {

  /** Busca statements por banco e período */
  List<Statement> findByBankAndCreatedAtBetween(
      Bank bank, LocalDateTime startDate, LocalDateTime endDate);

  /** Busca o statement mais recente de um banco específico */
  Optional<Statement> findTopByBankOrderByCreatedAtDesc(Bank bank);

  /**
   * Busca statements que tenham transações no período especificado Esta query é mais complexa e
   * busca statements que tenham pelo menos uma transação no período
   */
  @Query(
      "SELECT DISTINCT s FROM Statement s JOIN s.transactions t "
          + "WHERE s.bank = :bank AND t.transactionDate BETWEEN :startDate AND :endDate "
          + "ORDER BY s.createdAt DESC")
  List<Statement> findStatementsWithTransactionsInPeriod(
      @Param("bank") Bank bank,
      @Param("startDate") java.time.LocalDate startDate,
      @Param("endDate") java.time.LocalDate endDate);

  /**
   * Busca o statement que melhor cobre um período específico Retorna o statement com maior número
   * de transações no período solicitado
   */
  @Query(
      "SELECT s FROM Statement s JOIN s.transactions t "
          + "WHERE s.bank = :bank AND t.transactionDate BETWEEN :startDate AND :endDate "
          + "GROUP BY s.id "
          + "ORDER BY COUNT(t.id) DESC")
  List<Statement> findBestCoverageStatementsForPeriod(
      @Param("bank") Bank bank,
      @Param("startDate") java.time.LocalDate startDate,
      @Param("endDate") java.time.LocalDate endDate);
}
