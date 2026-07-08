package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionExportService {

  private final TransactionExcelExportService transactionExcelExportService;
  private final TransactionPdfExportService transactionPdfExportService;
  private final TransactionRepository transactionRepository;

  public Map<String, byte[]> exportTransactionsAsZip() throws IOException {
    Map<String, byte[]> excelData =
        transactionExcelExportService.exportTransactionsAsExcel(null, null);

    Map<String, byte[]> pdfData = transactionPdfExportService.exportTransactionsAsPdf();

    LocalDate now = LocalDate.now();
    LocalDate firstDayLastMonth = now.minusMonths(1).withDayOfMonth(1);
    LocalDate lastDayLastMonth = now.withDayOfMonth(1).minusDays(1);

    Pageable pageable = PageRequest.of(0, 10, Sort.by("transactionDate").descending());
    List<Transaction> bbTransactions =
        transactionRepository.findByTransactionDateBetweenAndStatementBank(
            firstDayLastMonth, lastDayLastMonth, Bank.BANCO_DO_BRASIL, pageable);

    List<Transaction> sicoobTransactions =
        transactionRepository.findByTransactionDateBetweenAndStatementBank(
            firstDayLastMonth, lastDayLastMonth, Bank.SICOOB, pageable);

    Set<Statement> referencedStatements = new HashSet<>();

    for (Transaction transaction : bbTransactions) {
      if (transaction.getStatement() != null) {
        referencedStatements.add(transaction.getStatement());
      }
    }

    for (Transaction transaction : sicoobTransactions) {
      if (transaction.getStatement() != null) {
        referencedStatements.add(transaction.getStatement());
      }
    }

    Map<String, byte[]> allFiles = new HashMap<>();

    String excelFileName = excelData.keySet().iterator().next();
    allFiles.put(excelFileName, excelData.get(excelFileName));

    String pdfFileName = pdfData.keySet().iterator().next();
    allFiles.put(pdfFileName, pdfData.get(pdfFileName));

    int statementCount = 1;
    for (Statement statement : referencedStatements) {
      if (statement.getFilePath() != null && statement.getFilePath().length > 0) {
        String statementFileName = sanitizeFileName(statement.getName());
        if (!statementFileName.toLowerCase().endsWith(".pdf")) {
          statementFileName += ".pdf";
        }

        String finalFileName =
            String.format(
                "%02d_%s_%s",
                statementCount++, statement.getBank().toString().toLowerCase(), statementFileName);

        allFiles.put(finalFileName, statement.getFilePath());
      }
    }

    return allFiles;
  }

  private String sanitizeFileName(String fileName) {
    if (fileName == null) {
      return "extrato_sem_nome";
    }

    String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    sanitized = sanitized.replaceAll("_{2,}", "_");
    sanitized = sanitized.replaceAll("^_+|_+$", "");

    if (sanitized.isEmpty()) {
      return "extrato_sem_nome";
    }

    return sanitized;
  }
}
