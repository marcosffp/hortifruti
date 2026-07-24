package com.hortifruti.sl.hortifruti.service.finance.transaction;

import com.hortifruti.sl.hortifruti.model.finance.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.StatementOrigin;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import com.hortifruti.sl.hortifruti.service.finance.StatementService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionExportService {

  private final TransactionReportService transactionReportService;
  private final TransactionRepository transactionRepository;
  private final StatementService statementService;

  public Map<String, byte[]> exportTransactionsAsZip() throws IOException {
    LocalDate now = LocalDate.now();
    LocalDate periodStart = now.minusMonths(1).withDayOfMonth(1);
    LocalDate periodEnd = now.withDayOfMonth(1).minusDays(1);

    byte[] excelBytes = transactionReportService.generateExcel(periodStart, periodEnd);
    byte[] pdfBytes = transactionReportService.generatePdf(periodStart, periodEnd);

    Pageable pageable = PageRequest.of(0, 10, Sort.by("transactionDate").descending());
    List<Transaction> bbTransactions =
        transactionRepository.findByTransactionDateBetweenAndStatementBank(
            periodStart, periodEnd, Bank.BANCO_DO_BRASIL, pageable);

    List<Transaction> sicoobTransactions =
        transactionRepository.findByTransactionDateBetweenAndStatementBank(
            periodStart, periodEnd, Bank.SICOOB, pageable);

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
    allFiles.put(
        transactionReportService.buildFileName(periodStart, periodEnd, "xlsx"), excelBytes);
    allFiles.put(transactionReportService.buildFileName(periodStart, periodEnd, "pdf"), pdfBytes);

    Map<Bank, List<Statement>> statementsByBank = new HashMap<>();
    for (Statement statement : referencedStatements) {
      statementsByBank.computeIfAbsent(statement.getBank(), b -> new ArrayList<>()).add(statement);
    }

    for (Map.Entry<Bank, List<Statement>> entry : statementsByBank.entrySet()) {
      Bank bank = entry.getKey();
      List<Statement> statements = entry.getValue();
      String label = bankFileLabel(bank);

      for (int i = 0; i < statements.size(); i++) {
        Statement statement = statements.get(i);
        String suffix = statements.size() > 1 ? "_" + (i + 1) : "";
        String baseName = "Extrato_Bancario_" + label + suffix;

        byte[] pdfContent = statementService.getFileContent(statement);
        if (pdfContent != null && pdfContent.length > 0) {
          allFiles.put(baseName + ".pdf", pdfContent);
        }

        if (statement.getOrigin() == StatementOrigin.API
            && statement.getPeriodStart() != null
            && statement.getPeriodEnd() != null) {
          try {
            byte[] excelContent = buildStatementExcel(bank, statement);
            if (excelContent != null && excelContent.length > 0) {
              allFiles.put(baseName + ".xlsx", excelContent);
            }
          } catch (Exception e) {
            log.error("Erro ao gerar excel do extrato {}", label, e);
          }
        }
      }
    }

    return allFiles;
  }

  private byte[] buildStatementExcel(Bank bank, Statement statement) throws IOException {
    LocalDate periodStart = statement.getPeriodStart();
    LocalDate periodEnd = statement.getPeriodEnd();
    return switch (bank) {
      case SICOOB ->
          statementService.exportSicoobExtratoExcel(
              periodStart.getMonthValue(),
              periodStart.getYear(),
              periodStart.getDayOfMonth(),
              periodEnd.getDayOfMonth());
      case BANCO_DO_BRASIL -> statementService.exportBBExtratoExcel(periodStart, periodEnd);
      case UNKNOWN -> null;
    };
  }

  private String bankFileLabel(Bank bank) {
    return switch (bank) {
      case SICOOB -> "Sicoob";
      case BANCO_DO_BRASIL -> "BancoDoBrasil";
      case UNKNOWN -> "Desconhecido";
    };
  }
}
