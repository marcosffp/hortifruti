package com.hortifruti.sl.hortifruti.scheduler;

import com.hortifruti.sl.hortifruti.repository.PurchaseRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseCleanupScheduler {

  private final PurchaseRepository purchaseRepository;

  // Executa uma vez por dia às 2:00 da manhã
  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public void deleteOldPurchases() {
    LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
    purchaseRepository.deleteByCreatedAtBefore(threeMonthsAgo);
  }
}
