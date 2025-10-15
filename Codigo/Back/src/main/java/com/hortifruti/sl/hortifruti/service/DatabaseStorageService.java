package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.service.notification.EmailTemplateService;
import com.hortifruti.sl.hortifruti.service.notification.NotificationCoordinator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseStorageService {

    @PersistenceContext
    private EntityManager entityManager;

    private final NotificationCoordinator notificationCoordinator;
    private final EmailTemplateService emailTemplateService;

    @Value("${overdue.notification.emails}")
    private String overdueNotificationEmails;

    private static final BigDecimal MAX_STORAGE_MB = new BigDecimal("5120"); // 5GB
    private static final BigDecimal THRESHOLD_PERCENTAGE = new BigDecimal("80"); // 80%
    private static final BigDecimal TARGET_PERCENTAGE = new BigDecimal("50"); // 50%

    public BigDecimal getDatabaseSizeInMB() {
        String query = "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS size_in_mb " +
                       "FROM information_schema.tables " +
                       "WHERE table_schema = DATABASE()";

        var result = entityManager.createNativeQuery(query).getResultList();
        if (!result.isEmpty() && result.get(0) != null) {
            return new BigDecimal(result.get(0).toString());
        }
        return BigDecimal.ZERO;
    }

    public boolean isDatabaseOverThreshold() {
        BigDecimal currentSize = getDatabaseSizeInMB();
        BigDecimal thresholdSize = MAX_STORAGE_MB.multiply(THRESHOLD_PERCENTAGE).divide(new BigDecimal("100"));
        return currentSize.compareTo(thresholdSize) >= 0;
    }

    public void cleanDatabaseIfNecessary() {
        BigDecimal currentSize = getDatabaseSizeInMB();
        BigDecimal thresholdSize = MAX_STORAGE_MB.multiply(THRESHOLD_PERCENTAGE).divide(new BigDecimal("100"));
        BigDecimal targetSize = MAX_STORAGE_MB.multiply(TARGET_PERCENTAGE).divide(new BigDecimal("100"));

        if (currentSize.compareTo(thresholdSize) >= 0) {
            log.warn("Banco de dados atingiu {} MB, excedendo o limite de {} MB", currentSize, thresholdSize);

            // Enviar notificação para a gerência
            sendNotificationToManagement(currentSize);

            // Remover dados antigos até atingir o tamanho alvo
            cleanOldData(targetSize);
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
        BigDecimal storagePercentage = currentSize.multiply(new BigDecimal("100"))
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

    private void cleanOldData(BigDecimal targetSize) {
        log.info("Iniciando limpeza de dados antigos para reduzir o tamanho do banco de dados...");

        // Data limite para remoção (4 meses atrás)
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(4);

        // Remover transações e statements antigos
        int deletedTransactions = entityManager.createQuery(
                "DELETE FROM Transaction t WHERE t.createdAt < :cutoffDate")
            .setParameter("cutoffDate", cutoffDate)
            .executeUpdate();

        int deletedStatements = entityManager.createQuery(
                "DELETE FROM Statement s WHERE s.createdAt < :cutoffDate")
            .setParameter("cutoffDate", cutoffDate)
            .executeUpdate();

        // Remover purchases e invoiceProducts antigos
        int deletedInvoiceProducts = entityManager.createQuery(
                "DELETE FROM InvoiceProduct ip WHERE ip.createdAt < :cutoffDate")
            .setParameter("cutoffDate", cutoffDate)
            .executeUpdate();

        int deletedPurchases = entityManager.createQuery(
                "DELETE FROM Purchase p WHERE p.purchaseDate < :cutoffDate")
            .setParameter("cutoffDate", cutoffDate)
            .executeUpdate();

        log.info("Limpeza concluída: {} transações, {} statements, {} produtos de fatura, {} compras removidos.",
                deletedTransactions, deletedStatements, deletedInvoiceProducts, deletedPurchases);

        // Verificar tamanho do banco após a limpeza
        BigDecimal newSize = getDatabaseSizeInMB();
        log.info("Tamanho do banco de dados após limpeza: {} MB", newSize);
    }

    /**
     * Método para teste manual do email de alerta de armazenamento
     */
    public void sendTestStorageNotification(BigDecimal simulatedSize) {
        log.info("Enviando email de teste de armazenamento com tamanho simulado: {} MB", simulatedSize);
        sendNotificationToManagement(simulatedSize);
    }
}