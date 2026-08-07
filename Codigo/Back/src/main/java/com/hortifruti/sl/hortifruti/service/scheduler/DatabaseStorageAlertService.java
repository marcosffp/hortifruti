package com.hortifruti.sl.hortifruti.service.scheduler;

import com.hortifruti.sl.hortifruti.service.notification.NotificationCoordinator;
import com.hortifruti.sl.hortifruti.service.notification.email.EmailTemplateService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Compõe e dispara o e-mail de alerta de armazenamento a partir do tamanho apurado por {@link
 * DatabaseStorageMonitorService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseStorageAlertService {

  private final DatabaseStorageMonitorService databaseStorageMonitorService;
  private final NotificationCoordinator notificationCoordinator;
  private final EmailTemplateService emailTemplateService;

  @Value("${overdue.notification.emails}")
  private String overdueNotificationEmails;

  private void sendNotificationToManagement(BigDecimal currentSize) {
    if (overdueNotificationEmails == null || overdueNotificationEmails.trim().isEmpty()) {
      log.warn("Nenhum email configurado para notificações de armazenamento");
      return;
    }

    String[] emails = overdueNotificationEmails.split(",");
    String subject = "Alerta: Armazenamento do Banco de Dados Excedido";

    BigDecimal maxStorageMB = databaseStorageMonitorService.getMaxStorageInMB();
    BigDecimal storagePercentage =
        currentSize
            .multiply(new BigDecimal("100"))
            .divide(maxStorageMB, 1, java.math.RoundingMode.HALF_UP);

    Map<String, String> variables = new HashMap<>();
    variables.put("STORAGE_PERCENTAGE", storagePercentage.toString());
    variables.put("CURRENT_SIZE", currentSize.toString());
    variables.put("MAX_SIZE", maxStorageMB.toString());

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

  public void sendTestStorageNotification(BigDecimal simulatedSize) {
    log.info("Enviando email de teste de armazenamento com tamanho simulado: {} MB", simulatedSize);
    sendNotificationToManagement(simulatedSize);
  }

  public void checkDatabaseStorage() {
    BigDecimal currentSize = databaseStorageMonitorService.getDatabaseSizeInMB();
    BigDecimal thresholdSize = databaseStorageMonitorService.getThresholdSizeInMB();

    if (currentSize.compareTo(thresholdSize) >= 0) {
      log.warn(
          "Banco de dados atingiu {} MB, excedendo o limite de {} MB", currentSize, thresholdSize);

      sendNotificationToManagement(currentSize);
    } else {
      log.info("Banco de dados dentro do limite configurado. Nenhuma ação necessária.");
    }
  }
}
