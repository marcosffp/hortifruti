package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
  Optional<Purchase> findTopByClientIdOrderByPurchaseDateDesc(Long clientId);

  // Soma real das compras do cliente — fonte da verdade para "Valor Total em Compras",
  // em vez de um contador mutável que pode dessincronizar (ver Client.totalPurchaseValue).
  @Query("SELECT COALESCE(SUM(p.total), 0) FROM Purchase p WHERE p.client.id = :clientId")
  BigDecimal sumTotalByClientId(@Param("clientId") Long clientId);

  @Query("SELECT p.client.id, COALESCE(SUM(p.total), 0) FROM Purchase p GROUP BY p.client.id")
  List<Object[]> sumTotalGroupedByClientId();

  List<Purchase> findByClientIdAndPurchaseDateBetween(
      Long clientId, LocalDateTime startDate, LocalDateTime endDate);

  List<Purchase> findByClientId(Long clientId);

  void deleteByCreatedAtBefore(LocalDateTime dateTime);

  Page<Purchase> findByClientIdOrderByPurchaseDateDesc(
      @Param("clientId") Long clientId, Pageable pageable);

  Page<Purchase> findByClientIdOrderByCreatedAtDesc(
      @Param("clientId") Long clientId, Pageable pageable);

  List<Purchase> findByIdIn(@Param("ids") List<Long> ids);

  Page<Purchase> findByPurchaseDateBetweenOrderByPurchaseDateDesc(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  List<Purchase> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  List<Purchase> findByCombinedScoreIdAndImagemR2KeyIsNotNull(Long combinedScoreId);

  boolean existsByImagemR2Key(String imagemR2Key);
}
