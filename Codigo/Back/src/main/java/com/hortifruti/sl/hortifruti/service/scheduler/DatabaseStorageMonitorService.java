package com.hortifruti.sl.hortifruti.service.scheduler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Consulta o tamanho atual do banco e o compara com o limite configurado — sem depender de nada de
 * notificação/e-mail.
 */
@Service
public class DatabaseStorageMonitorService {

  @PersistenceContext private EntityManager entityManager;

  private static final BigDecimal MAX_STORAGE_MB = new BigDecimal("1024");
  private static final BigDecimal THRESHOLD_PERCENTAGE = new BigDecimal("80");

  public BigDecimal getDatabaseSizeInMB() {
    String query =
        "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS size_in_mb "
            + "FROM information_schema.tables "
            + "WHERE table_schema = DATABASE()";

    var result = entityManager.createNativeQuery(query).getResultList();
    if (!result.isEmpty() && result.get(0) != null) {
      return new BigDecimal(result.get(0).toString());
    }
    return BigDecimal.ZERO;
  }

  public BigDecimal getMaxStorageInMB() {
    return MAX_STORAGE_MB;
  }

  public BigDecimal getThresholdSizeInMB() {
    return MAX_STORAGE_MB.multiply(THRESHOLD_PERCENTAGE).divide(new BigDecimal("100"));
  }

  public boolean isDatabaseOverThreshold() {
    return getDatabaseSizeInMB().compareTo(getThresholdSizeInMB()) >= 0;
  }
}
