package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.service.finance.StatementService;
import com.hortifruti.sl.hortifruti.service.finance.transaction.TransactionProcessingService;
import com.hortifruti.sl.hortifruti.service.purchase.InvoiceProductService;
import com.hortifruti.sl.hortifruti.service.purchase.PurchaseService;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class EntityCleanupService {

  private final PurchaseService purchaseService;
  private final InvoiceProductService invoiceProductService;
  private final TransactionProcessingService transactionProcessingService;
  private final StatementService statementService;

  @Transactional
  public void cleanupEntitiesForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
    try {

      cleanupInvoiceProducts(startDate, endDate);

      cleanupPurchases(startDate, endDate);

      cleanupTransactions(startDate, endDate);

      cleanupStatements(startDate, endDate);

    } catch (Exception e) {
      throw new BackupException("Erro ao remover entidades do banco de dados.", e);
    }
  }

  private void cleanupPurchases(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      purchaseService.deleteAllByCreatedAtBetween(startDate, endDate);
    } catch (Exception e) {
      throw new BackupException("Erro ao remover compras.", e);
    }
  }

  private void cleanupInvoiceProducts(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      invoiceProductService.deleteAllByCreatedAtBetween(startDate, endDate);
    } catch (Exception e) {
      throw new BackupException("Erro ao remover produtos de fatura.", e);
    }
  }

  private void cleanupTransactions(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      transactionProcessingService.deleteAllByCreatedAtBetween(startDate, endDate);
    } catch (Exception e) {
      throw new BackupException("Erro ao remover transações.", e);
    }
  }

  private void cleanupStatements(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      statementService.deleteAllByCreatedAtBetween(startDate, endDate);
    } catch (Exception e) {
      throw new BackupException("Erro ao remover extratos.", e);
    }
  }
}
