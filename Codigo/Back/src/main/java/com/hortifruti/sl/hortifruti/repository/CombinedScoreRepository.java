package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.CombinedScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CombinedScoreRepository extends JpaRepository<CombinedScore, Long> {

  Page<CombinedScore> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
