package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.exception.TransactionException;
import com.hortifruti.sl.hortifruti.mapper.TransactionMapper;
import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import com.hortifruti.sl.hortifruti.util.PdfUtil;
import com.hortifruti.sl.hortifruti.util.TransactionUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class TransactionSicoobService {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{2}/\\d{2})");
  private static final Pattern VALUE_PATTERN = Pattern.compile("R\\$\\s*([\\d.,]+)([DC])");

  protected List<Transaction> importStatement(MultipartFile file, Statement statement)
      throws IOException {
    String text = PdfUtil.extractPdfText(file);
    List<Transaction> transactions = parseSicoob(text, statement);

    List<Transaction> newTransactions =
        TransactionUtil.filterNewTransactions(transactions, transactionRepository);

    if (newTransactions.isEmpty()) {
      return new ArrayList<>();
    }

    try {
      List<Transaction> savedTransactions = transactionRepository.saveAll(newTransactions);
      return savedTransactions;
    } catch (DataIntegrityViolationException e) {

      return saveTransactionsIndividually(newTransactions);
    }
  }

  private List<Transaction> saveTransactionsIndividually(List<Transaction> transactions) {
    List<Transaction> savedTransactions = new ArrayList<>();

    for (Transaction transaction : transactions) {
      try {
        Transaction saved = transactionRepository.save(transaction);
        savedTransactions.add(saved);
      } catch (Exception e) {
        throw new TransactionException("Erro ao salvar transação: " + transaction.toString(), e);
      }
    }

    return savedTransactions;
  }

  private List<Transaction> parseSicoob(String text, Statement statement) {
    List<Transaction> transactions = new ArrayList<>();
    String[] lines = text.split("\n");

    LocalDate currentDate = null;
    StringBuilder historyBuffer = new StringBuilder();
    String document = null;

    for (String rawLine : lines) {
      String line = rawLine.trim();

      if (line.isBlank()
          || line.contains("SALDO DO DIA")
          || line.contains("SALDO ANTERIOR")
          || line.matches("\\d{2}/\\d{2}/\\d{2}, \\d{2}:\\d{2}")
          || line.contains("Sicoob | Internet Banking")) {
        continue;
      }

      Matcher dateMatcher = DATE_PATTERN.matcher(line);
      if (dateMatcher.find()) {
        if (currentDate != null && historyBuffer.length() > 0) {
          Transaction transaction =
              createTransaction(currentDate, document, historyBuffer.toString().trim(), statement);
          if (transaction.getHash() != null && !transaction.getHash().isEmpty()) {
            transactions.add(transaction);
          }
          historyBuffer.setLength(0);
        }

        String[] parts = line.split("\\s+", 3);
        currentDate = TransactionUtil.parseDate(parts[0]);
        document = parts.length > 1 ? parts[1] : null;
        if (parts.length > 2) {
          historyBuffer.append(parts[2]).append(" ");
        }
        continue;
      }

      Matcher valueMatcher = VALUE_PATTERN.matcher(line);
      if (valueMatcher.find() && currentDate != null) {
        Transaction transaction =
            createTransactionFromMatcher(
                currentDate, document, historyBuffer.toString().trim(), valueMatcher, statement);
        if (transaction.getHash() != null && !transaction.getHash().isEmpty()) {
          transactions.add(transaction);
        }
        historyBuffer.setLength(0);
        document = null;
        currentDate = null;
      } else if (currentDate != null) {
        historyBuffer.append(line).append(" ");
      }
    }

    if (currentDate != null && historyBuffer.length() > 0) {
      Transaction transaction =
          createTransaction(currentDate, document, historyBuffer.toString().trim(), statement);
      if (transaction.getHash() != null && !transaction.getHash().isEmpty()) {
        transactions.add(transaction);
      }
    }

    return transactions;
  }

  private Transaction createTransactionFromMatcher(
      LocalDate date, String document, String history, Matcher valueMatcher, Statement statement) {
    String type = valueMatcher.group(2);
    BigDecimal amount = TransactionUtil.parseAmount(valueMatcher.group(1), type);
    TransactionType transactionType = TransactionUtil.determineTransactionType(type);
    Category category = TransactionUtil.determineCategory(history.toLowerCase(), type);
    history = cleanDescription(history);

    Transaction transaction =
        transactionMapper.toTransaction(
            statement,
            "",
            history,
            amount,
            category,
            transactionType,
            document,
            "",
            "",
            date.toString());

    String hash =
        TransactionUtil.generateTransactionHash(
            date, document != null ? document : "", amount, history);
    transaction.setHash(hash);

    return transaction;
  }

  private Transaction createTransaction(
      LocalDate date, String document, String history, Statement statement) {
    Matcher valueMatcher = VALUE_PATTERN.matcher(history);
    if (valueMatcher.find()) {
      return createTransactionFromMatcher(date, document, history, valueMatcher, statement);
    }

    Transaction transaction =
        transactionMapper.toTransaction(
            statement,
            "",
            history,
            BigDecimal.ZERO,
            Category.FORNECEDOR,
            TransactionType.DEBITO,
            document,
            "",
            "",
            date.toString());

    String hash =
        TransactionUtil.generateTransactionHash(
            date, document != null ? document : "", BigDecimal.ZERO, history);
    transaction.setHash(hash);

    return transaction;
  }

  private String cleanDescription(String description) {
    return description.replaceAll("R\\$\\s*[\\d.,]+[DC]", "").trim();
  }
}
