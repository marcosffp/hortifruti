package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CombinedScoreRepository extends JpaRepository<CombinedScore, Long> {

  Page<CombinedScore> findByClientIdOrderByConfirmedAtDesc(Long clientId, Pageable pageable);

  Page<CombinedScore> findAllByOrderByConfirmedAtDesc(Pageable pageable);

  /** Busca CombinedScores vencidos que ainda não foram pagos (confirmedAt is null) */
  @Query(
      "SELECT cs FROM CombinedScore cs WHERE cs.dueDate < :currentDate AND cs.confirmedAt IS NULL")
  List<CombinedScore> findOverdueUnpaidScores(@Param("currentDate") LocalDate currentDate);

  /** Busca CombinedScores vencidos por cliente */
  @Query(
      "SELECT cs FROM CombinedScore cs WHERE cs.clientId = :clientId AND cs.dueDate < :currentDate AND cs.confirmedAt IS NULL")
  List<CombinedScore> findOverdueUnpaidScoresByClient(
      @Param("clientId") Long clientId, @Param("currentDate") LocalDate currentDate);

  Optional<CombinedScore> findByYourNumber(String yourNumber);

  Optional<CombinedScore> findByInvoiceRef(String invoiceRef);

  @Query("SELECT cs FROM CombinedScore cs WHERE cs.status = 'PENDENTE' AND cs.dueDate <= :date")
  List<CombinedScore> findOverduePendingScores(@Param("date") LocalDate date);

  @Query("SELECT cs FROM CombinedScore cs WHERE cs.hasInvoice = true AND cs.confirmedAt BETWEEN :startDate AND :endDate")
  List<CombinedScore> findByHasInvoiceTrueAndConfirmedAtBetween(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
