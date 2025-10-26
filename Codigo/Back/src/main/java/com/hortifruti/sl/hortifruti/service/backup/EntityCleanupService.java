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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
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
      log.info("Iniciando limpeza de entidades para o período: {} a {}", startDate, endDate);

      log.info("Iniciando limpeza dos produtos de fatura");
      cleanupInvoiceProducts(startDate, endDate);

      log.info("Iniciando limpeza das compras");
      cleanupPurchases(startDate, endDate);

      log.info("Iniciando limpeza das transações");
      cleanupTransactions(startDate, endDate);

      log.info("Iniciando limpeza dos extratos");
      cleanupStatements(startDate, endDate);

      log.info("Limpeza de entidades concluída com sucesso.");
    } catch (Exception e) {
      log.error("Erro durante a limpeza de entidades: {}", e.getMessage(), e);
      throw new BackupException("Erro ao remover entidades do banco de dados.", e);
    }
  }

  private void cleanupPurchases(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      List<Purchase> purchases = purchaseRepository.findByCreatedAtBetween(startDate, endDate);
      purchaseRepository.deleteAll(purchases);
      log.info("Compras removidas para o período: {} a {}", startDate, endDate);
    } catch (Exception e) {
      log.error("Erro ao remover compras: {}", e.getMessage(), e);
      throw new BackupException("Erro ao remover compras.", e);
    }
  }

  private void cleanupInvoiceProducts(LocalDateTime startDate, LocalDateTime endDate) {
    try {

      invoiceProductRepository.deleteByCreatedAtBetween(startDate, endDate);
      log.info(
          "Produtos de fatura removidos: {} registros para o período: {} a {}", startDate, endDate);
    } catch (Exception e) {
      log.error("Erro ao remover produtos de fatura: {}", e.getMessage(), e);
      throw new BackupException("Erro ao remover produtos de fatura.", e);
    }
  }

  private void cleanupTransactions(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      List<com.hortifruti.sl.hortifruti.model.finance.Transaction> transactions =
          transactionRepository.findTransactionsByCreatedAtBetween(startDate, endDate);

      transactionRepository.deleteAll(transactions);
      log.info(
          "Transações removidas: {} registros para o período: {} a {}",
          transactions.size(),
          startDate,
          endDate);
    } catch (Exception e) {
      log.error("Erro ao remover transações: {}", e.getMessage(), e);
      throw new BackupException("Erro ao remover transações.", e);
    }
  }

  private void cleanupStatements(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      List<Statement> statements =
          statementRepository.findByCreatedAtBetweenWithTransactions(startDate, endDate);

      statementRepository.deleteAll(statements);
      log.info(
          "Extratos removidos: {} registros para o período: {} a {}",
          statements.size(),
          startDate,
          endDate);
    } catch (Exception e) {
      log.error("Erro ao remover extratos: {}", e.getMessage(), e);
      throw new BackupException("Erro ao remover extratos.", e);
    }
  }
}
