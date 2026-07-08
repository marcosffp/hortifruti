package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceProductRepository extends JpaRepository<InvoiceProduct, Long> {
  void deleteByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
