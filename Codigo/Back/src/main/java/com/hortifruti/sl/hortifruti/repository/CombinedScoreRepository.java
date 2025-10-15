package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.CombinedScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CombinedScoreRepository extends JpaRepository<CombinedScore, Long> {

  Page<CombinedScore> findByClientIdOrderByConfirmedAtDesc(Long clientId, Pageable pageable);

  Page<CombinedScore> findAllByOrderByConfirmedAtDesc(Pageable pageable);

  /**
   * Busca CombinedScores vencidos que ainda não foram pagos (confirmedAt is null)
   */
  @Query("SELECT cs FROM CombinedScore cs WHERE cs.dueDate < :currentDate AND cs.confirmedAt IS NULL")
  List<CombinedScore> findOverdueUnpaidScores(@Param("currentDate") LocalDate currentDate);
  
  /**
   * Busca CombinedScores vencidos por cliente
   */
  @Query("SELECT cs FROM CombinedScore cs WHERE cs.clientId = :clientId AND cs.dueDate < :currentDate AND cs.confirmedAt IS NULL")
  List<CombinedScore> findOverdueUnpaidScoresByClient(@Param("clientId") Long clientId, @Param("currentDate") LocalDate currentDate);
}