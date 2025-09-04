package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.Transaction;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  @Query("SELECT t.hash FROM Transaction t WHERE t.hash IN :hashes")
  Set<String> findHashes(@Param("hashes") Set<String> hashes);
}
