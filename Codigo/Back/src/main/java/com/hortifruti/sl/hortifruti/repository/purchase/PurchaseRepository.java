package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
  Optional<Purchase> findTopByClientIdOrderByPurchaseDateDesc(Long clientId);

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
}
