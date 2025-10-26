package com.hortifruti.sl.hortifruti.service.scheduler;

import com.hortifruti.sl.hortifruti.service.notification.EmailTemplateService;
import com.hortifruti.sl.hortifruti.service.notification.NotificationCoordinator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseStorageService {

  @PersistenceContext private EntityManager entityManager;

  private final NotificationCoordinator notificationCoordinator;
  private final EmailTemplateService emailTemplateService;

  @Value("${overdue.notification.emails}")
  private String overdueNotificationEmails;

  private static final BigDecimal MAX_STORAGE_MB = new BigDecimal("5120"); // 5GB
  private static final BigDecimal THRESHOLD_PERCENTAGE = new BigDecimal("80"); // 80%

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

  public boolean isDatabaseOverThreshold() {
    BigDecimal currentSize = getDatabaseSizeInMB();
    BigDecimal thresholdSize =
        MAX_STORAGE_MB.multiply(THRESHOLD_PERCENTAGE).divide(new BigDecimal("100"));
    return currentSize.compareTo(thresholdSize) >= 0;
  }

  public void cleanDatabaseIfNecessary() {
    BigDecimal currentSize = getDatabaseSizeInMB();
    BigDecimal thresholdSize =
        MAX_STORAGE_MB.multiply(THRESHOLD_PERCENTAGE).divide(new BigDecimal("100"));

    if (currentSize.compareTo(thresholdSize) >= 0) {
      log.warn(
          "Banco de dados atingiu {} MB, excedendo o limite de {} MB", currentSize, thresholdSize);

      // Enviar notificação para a gerência
      sendNotificationToManagement(currentSize);
    }
  }

  private void sendNotificationToManagement(BigDecimal currentSize) {
    if (overdueNotificationEmails == null || overdueNotificationEmails.trim().isEmpty()) {
      log.warn("Nenhum email configurado para notificações de armazenamento");
      return;
    }

    String[] emails = overdueNotificationEmails.split(",");
    String subject = "Alerta: Armazenamento do Banco de Dados Excedido";

    // Calcular percentual de uso
    BigDecimal storagePercentage =
        currentSize
            .multiply(new BigDecimal("100"))
            .divide(MAX_STORAGE_MB, 1, java.math.RoundingMode.HALF_UP);

    // Preparar variáveis para o template
    Map<String, String> variables = new HashMap<>();
    variables.put("STORAGE_PERCENTAGE", storagePercentage.toString());
    variables.put("CURRENT_SIZE", currentSize.toString());
    variables.put("MAX_SIZE", MAX_STORAGE_MB.toString());

    // Processar template HTML
    String emailBody = emailTemplateService.processTemplate("database-management", variables);

    for (String email : emails) {
      try {
        notificationCoordinator.sendEmailOnly(email.trim(), subject, emailBody, null, null);
        log.info("Notificação de armazenamento enviada para {}", email.trim());
      } catch (Exception e) {
        log.error("Erro ao enviar notificação de armazenamento para {}", email.trim(), e);
      }
    }
  }

  /** Método para teste manual do email de alerta de armazenamento */
  public void sendTestStorageNotification(BigDecimal simulatedSize) {
    log.info("Enviando email de teste de armazenamento com tamanho simulado: {} MB", simulatedSize);
    sendNotificationToManagement(simulatedSize);
  }

  public void checkDatabaseStorage() {
    BigDecimal currentSize = getDatabaseSizeInMB();
    BigDecimal thresholdSize =
        MAX_STORAGE_MB.multiply(THRESHOLD_PERCENTAGE).divide(new BigDecimal("100"));

    if (currentSize.compareTo(thresholdSize) >= 0) {
      log.warn(
          "Banco de dados atingiu {} MB, excedendo o limite de {} MB", currentSize, thresholdSize);

      // Enviar notificação para a gerência
      sendNotificationToManagement(currentSize);
    } else {
      log.info("Banco de dados dentro do limite configurado. Nenhuma ação necessária.");
    }
  }
}
