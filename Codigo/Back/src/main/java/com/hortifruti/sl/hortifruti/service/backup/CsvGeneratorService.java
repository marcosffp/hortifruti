package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.finance.StatementRepository;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.InvoiceProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CsvGeneratorService {

  private final PurchaseRepository purchaseRepository;
  private final InvoiceProductRepository invoiceProductRepository;
  private final TransactionRepository transactionRepository;
  private final StatementRepository statementRepository;

  /**
   * Gera arquivos CSV para as entidades especificadas dentro de um período.
   *
   * @param startDate Data inicial do período.
   * @param endDate Data final do período.
   * @return Lista de caminhos dos arquivos CSV gerados.
   */
  public List<String> generateCSVsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
    log.info("Iniciando geração de arquivos CSV para o período: {} a {}", startDate, endDate);
    String tempDir = System.getProperty("java.io.tmpdir");
    log.debug("Diretório temporário para os arquivos CSV: {}", tempDir);

    List<String> csvFiles = new ArrayList<>();
    try {
      csvFiles.add(generatePurchasesCSV(startDate, endDate, tempDir));
      csvFiles.add(generateInvoiceProductsCSV(startDate, endDate, tempDir));
      csvFiles.add(generateTransactionsCSV(startDate, endDate, tempDir));
      csvFiles.add(generateStatementsCSV(startDate, endDate, tempDir));
    } catch (Exception e) {
      log.error("Erro durante a geração de arquivos CSV: {}", e.getMessage(), e);
      throw new BackupException("Erro ao gerar arquivos CSV para o período especificado.", e);
    }

    log.info("Arquivos CSV gerados com sucesso: {}", csvFiles);
    return csvFiles;
  }

  private String generatePurchasesCSV(
      LocalDateTime startDate, LocalDateTime endDate, String tempDir) {
    log.info("Gerando CSV de compras para o período: {} a {}", startDate, endDate);
    Path filePath =
        Paths.get(
            tempDir, "compras_" + startDate.toLocalDate() + "_a_" + endDate.toLocalDate() + ".csv");
    log.debug("Caminho do arquivo CSV de compras: {}", filePath);

    List<Purchase> purchases = purchaseRepository.findByCreatedAtBetween(startDate, endDate);
    log.info("Compras encontradas: {}", purchases.size());

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID", "Cliente ID", "Data da Compra", "Valor Total", "Criado Em", "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (Purchase purchase : purchases) {
        log.debug("Escrevendo compra no CSV: {}", purchase);
        csvPrinter.printRecord(
            purchase.getId(),
            purchase.getClient().getId(),
            purchase.getPurchaseDate(),
            purchase.getTotal(),
            purchase.getCreatedAt(),
            purchase.getUpdatedAt());
      }

      csvPrinter.flush();
      log.info("CSV de compras gerado com sucesso: {}", filePath);
      return filePath.toString();
    } catch (IOException e) {
      log.error("Erro ao gerar o arquivo CSV de compras: {}", e.getMessage(), e);
      throw new BackupException("Erro ao gerar o arquivo CSV de compras.", e);
    }
  }

  private String generateInvoiceProductsCSV(
      LocalDateTime startDate, LocalDateTime endDate, String tempDir) {
    log.info("Gerando CSV de produtos da fatura para o período: {} a {}", startDate, endDate);
    Path filePath =
        Paths.get(
            tempDir,
            "produtos_do_pedido_"
                + startDate.toLocalDate()
                + "_a_"
                + endDate.toLocalDate()
                + ".csv");
    log.debug("Caminho do arquivo CSV de produtos da fatura: {}", filePath);

    List<InvoiceProduct> invoiceProducts =
        invoiceProductRepository.findAll(); // Ajustar se necessário para filtrar por data
    log.info("Produtos da fatura encontrados: {}", invoiceProducts.size());

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID",
                "Código",
                "Nome",
                "Preço",
                "Tipo de Unidade",
                "Quantidade",
                "Compra ID",
                "Criado Em",
                "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (InvoiceProduct product : invoiceProducts) {
        log.debug("Escrevendo produto da fatura no CSV: {}", product);
        csvPrinter.printRecord(
            product.getId(),
            product.getCode(),
            product.getName(),
            product.getPrice(),
            product.getUnitType(),
            product.getQuantity(),
            product.getPurchase().getId(),
            product.getCreatedAt(),
            product.getUpdatedAt());
      }

      csvPrinter.flush();
      log.info("CSV de produtos da fatura gerado com sucesso: {}", filePath);
      return filePath.toString();
    } catch (IOException e) {
      log.error("Erro ao gerar o arquivo CSV de produtos da fatura: {}", e.getMessage(), e);
      throw new BackupException("Erro ao gerar o arquivo CSV de produtos da fatura.", e);
    }
  }

  private String generateTransactionsCSV(
      LocalDateTime startDate, LocalDateTime endDate, String tempDir) {
    log.info("Gerando CSV de transações para o período: {} a {}", startDate, endDate);
    Path filePath =
        Paths.get(
            tempDir,
            "transacoes_" + startDate.toLocalDate() + "_a_" + endDate.toLocalDate() + ".csv");
    log.debug("Caminho do arquivo CSV de transações: {}", filePath);

    List<Transaction> transactions =
        transactionRepository.findTransactionsByCreatedAtBetween(startDate, endDate);
    log.info("Transações encontradas: {}", transactions.size());

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID",
                "Data da Transação",
                "Código Histórico",
                "Histórico",
                "Valor",
                "Categoria",
                "Tipo de Transação",
                "Documento",
                "Agência de Origem",
                "Lote",
                "Hash",
                "Criado Em",
                "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (Transaction transaction : transactions) {
        log.debug("Escrevendo transação no CSV: {}", transaction);
        csvPrinter.printRecord(
            transaction.getId(),
            transaction.getTransactionDate(),
            transaction.getCodHistory(),
            transaction.getHistory(),
            transaction.getAmount(),
            transaction.getCategory(),
            transaction.getTransactionType(),
            transaction.getDocument(),
            transaction.getSourceAgency(),
            transaction.getBatch(),
            transaction.getHash(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt());
      }

      csvPrinter.flush();
      log.info("CSV de transações gerado com sucesso: {}", filePath);
      return filePath.toString();
    } catch (IOException e) {
      log.error("Erro ao gerar o arquivo CSV de transações: {}", e.getMessage(), e);
      throw new BackupException("Erro ao gerar o arquivo CSV de transações.", e);
    }
  }

  private String generateStatementsCSV(
      LocalDateTime startDate, LocalDateTime endDate, String tempDir) {
    log.info("Gerando CSV de extratos para o período: {} a {}", startDate, endDate);
    Path filePath =
        Paths.get(
            tempDir,
            "extratos_" + startDate.toLocalDate() + "_a_" + endDate.toLocalDate() + ".csv");
    log.debug("Caminho do arquivo CSV de extratos: {}", filePath);

    List<Statement> statements = statementRepository.findByCreatedAtBetween(startDate, endDate);
    log.info("Extratos encontrados: {}", statements.size());

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader("ID", "Nome", "Banco", "Criado Em", "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (Statement statement : statements) {
        log.debug("Escrevendo extrato no CSV: {}", statement);
        csvPrinter.printRecord(
            statement.getId(),
            statement.getName(),
            statement.getBank(),
            statement.getCreatedAt(),
            statement.getUpdatedAt());
      }

      csvPrinter.flush();
      log.info("CSV de extratos gerado com sucesso: {}", filePath);
      return filePath.toString();
    } catch (IOException e) {
      log.error("Erro ao gerar o arquivo CSV de extratos: {}", e.getMessage(), e);
      throw new BackupException("Erro ao gerar o arquivo CSV de extratos.", e);
    }
  }
}
