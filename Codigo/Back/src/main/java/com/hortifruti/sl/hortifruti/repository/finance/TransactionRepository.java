package com.hortifruti.sl.hortifruti.repository.finance;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository
    extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

  @Query("SELECT t.hash FROM Transaction t WHERE t.hash IN :hashes")
  Set<String> findHashes(@Param("hashes") Set<String> hashes);

  @Query(
      """
          SELECT t FROM Transaction t
          WHERE
            t.transactionDate >= :startDate
            AND t.transactionDate <= :endDate
            AND t.statement.bank = :bank
      """)
  List<Transaction> findByTransactionDateBetweenAndStatementBank(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("bank") Bank bank);

  @Query("SELECT DISTINCT t.category FROM Transaction t WHERE t.category IS NOT NULL")
  List<String> findAllCategories();

  @Query(
      """
        SELECT t
        FROM Transaction t
        WHERE t.transactionDate >= :startDate
          AND t.transactionDate <= :endDate
      """)
  List<Transaction> findTransactionsByDateRange(
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  boolean existsByHash(String hash);

  List<Transaction> findByTransactionDateBetweenAndTransactionType(
      LocalDate startDate, LocalDate endDate, TransactionType type);

  @Query(
      """
      SELECT t
      FROM Transaction t
      WHERE t.transactionDate >= :startDate
        AND t.transactionDate <= :endDate
        AND t.category = :category
      """)
  List<Transaction> findByTransactionDateBetweenAndCategory(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("category") Category category);
}
