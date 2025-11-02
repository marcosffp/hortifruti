package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.finance.StatementRepository;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.InvoiceProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class EntityCleanupService {

  private final PurchaseRepository purchaseRepository;
  private final InvoiceProductRepository invoiceProductRepository;
  private final TransactionRepository transactionRepository;
  private final StatementRepository statementRepository;

  /**
   * Remove entidades do banco de dados com base no período especificado.
   *
   * @param startDate Data inicial do período.
   * @param endDate Data final do período.
   */
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
      List<Purchase> purchases = purchaseRepository.findByCreatedAtBetween(startDate, endDate);
      purchaseRepository.deleteAll(purchases);
    } catch (Exception e) {
      throw new BackupException("Erro ao remover compras.", e);
    }
  }

  private void cleanupInvoiceProducts(LocalDateTime startDate, LocalDateTime endDate) {
    try {

      invoiceProductRepository.deleteByCreatedAtBetween(startDate, endDate);
    } catch (Exception e) {
      throw new BackupException("Erro ao remover produtos de fatura.", e);
    }
  }

  private void cleanupTransactions(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      List<com.hortifruti.sl.hortifruti.model.finance.Transaction> transactions =
          transactionRepository.findTransactionsByCreatedAtBetween(startDate, endDate);

      transactionRepository.deleteAll(transactions);

    } catch (Exception e) {
      throw new BackupException("Erro ao remover transações.", e);
    }
  }

  private void cleanupStatements(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      List<Statement> statements =
          statementRepository.findByCreatedAtBetweenWithTransactions(startDate, endDate);

      statementRepository.deleteAll(statements);
    } catch (Exception e) {
      throw new BackupException("Erro ao remover extratos.", e);
    }
  }
}
