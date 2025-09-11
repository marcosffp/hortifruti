package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

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
            WHERE YEAR(t.transactionDate) = YEAR(CURRENT_DATE)
              AND MONTH(t.transactionDate) = MONTH(CURRENT_DATE)
      """)
  List<Transaction> findTransactionsForCurrentMonth();
}
