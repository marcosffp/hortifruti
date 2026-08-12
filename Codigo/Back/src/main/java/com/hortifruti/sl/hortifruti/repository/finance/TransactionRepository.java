package com.hortifruti.sl.hortifruti.repository.finance;

import com.hortifruti.sl.hortifruti.model.finance.Category;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
    extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

  @Query("SELECT t.hash FROM Transaction t WHERE t.hash IN :hashes")
  Set<String> findHashes(@Param("hashes") Set<String> hashes);

  @Query("SELECT DISTINCT t.category FROM Transaction t WHERE t.category IS NOT NULL")
  List<Category> findAllCategories();
}
