package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {}
