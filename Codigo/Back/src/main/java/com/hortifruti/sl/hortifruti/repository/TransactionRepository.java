package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {}
