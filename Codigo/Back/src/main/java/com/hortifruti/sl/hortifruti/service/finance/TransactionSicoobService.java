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

@Service
@AllArgsConstructor
public class TransactionSicoobService {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{2}/\\d{2})");
  private static final Pattern VALUE_PATTERN = Pattern.compile("R\\$\\s*([\\d.,]+)([DC])");

  protected List<Transaction> importStatement(
      byte[] fileBytes, String fileName, Statement statement) throws IOException {

    String text = PdfUtil.extractPdfText(fileBytes);

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

    for (int i = 0; i < transactions.size(); i++) {
      Transaction transaction = transactions.get(i);
      try {

        if (transaction.getHistory() != null && transaction.getHistory().length() > 500) {
          transaction.setHistory(transaction.getHistory().substring(0, 497) + "...");
        }

        transaction.setId(null);

        Transaction saved = transactionRepository.save(transaction);
        savedTransactions.add(saved);
      } catch (Exception e) {

        throw new TransactionException(
            "Erro ao salvar transação " + (i + 1) + ": " + e.getMessage(), e);
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

    for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      String rawLine = lines[lineIndex];
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
          String historyContent = historyBuffer.toString().trim();
          if (historyContent.length() > 200) {}

          Transaction transaction =
              createTransaction(currentDate, document, historyContent, statement);
          if (transaction != null
              && transaction.getHash() != null
              && !transaction.getHash().isEmpty()) {
            transactions.add(transaction);
          }
          historyBuffer.setLength(0);
        }

        String[] parts = line.split("\\s+", 3);
        currentDate = TransactionUtil.parseDate(parts[0]);
        document = parts.length > 1 ? parts[1] : null;

        Matcher immediateValueMatcher = VALUE_PATTERN.matcher(line);
        if (immediateValueMatcher.find()) {

          String lineHistory = "";
          if (parts.length > 2) {
            String remainingLine = line.substring(parts[0].length() + parts[1].length()).trim();
            lineHistory = remainingLine;
          }

          Transaction transaction =
              createTransactionFromMatcher(
                  currentDate, document, lineHistory, immediateValueMatcher, statement);
          if (transaction != null
              && transaction.getHash() != null
              && !transaction.getHash().isEmpty()) {
            transactions.add(transaction);
          }

          currentDate = null;
          document = null;
          continue;
        }
        if (parts.length > 2) {
          historyBuffer.append(parts[2]).append(" ");
        }
        continue;
      }

      Matcher valueMatcher = VALUE_PATTERN.matcher(line);
      if (valueMatcher.find() && currentDate != null) {

        String historicalPart = line.replaceAll("R\\$\\s*[\\d.,]+[DC]", "").trim();
        historyBuffer.append(historicalPart).append(" ");

        Transaction transaction =
            createTransactionFromMatcher(
                currentDate, document, historyBuffer.toString().trim(), valueMatcher, statement);
        if (transaction != null
            && transaction.getHash() != null
            && !transaction.getHash().isEmpty()) {
          transactions.add(transaction);
        }
        historyBuffer.setLength(0);
        document = null;
        currentDate = null;
      } else if (currentDate != null) {
        historyBuffer.append(line).append(" ");

        if (historyBuffer.length() > 1000) {
          historyBuffer.setLength(500);
          historyBuffer.append("...");
        }
      }
    }

    if (currentDate != null && historyBuffer.length() > 0) {
      String historyContent = historyBuffer.toString().trim();
      Transaction transaction = createTransaction(currentDate, document, historyContent, statement);
      if (transaction != null
          && transaction.getHash() != null
          && !transaction.getHash().isEmpty()) {
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

    if (history != null && history.length() > 500) {
      history = history.substring(0, 497) + "...";
    }

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

    BigDecimal amount = BigDecimal.ZERO;
    TransactionType transactionType = TransactionType.DEBITO;
    String cleanHistory = history;

    Pattern[] valuePatterns = {
      Pattern.compile("R\\$\\s*([\\d.,]+)([DC])"), // R$ 1,26D
      Pattern.compile("R\\$\\s*([\\d.,]+)\\s*([DC])"), // R$ 1,26 D
      Pattern.compile("R\\$\\s*([\\d.,]+)([DC]?)"), // R$ 1,26 (sem DC)
      Pattern.compile(
          "([\\d]{1,3}(?:\\.[\\d]{3})*,[\\d]{2})([DC])"), // 1.000,00D (formato brasileiro)
      Pattern.compile("([\\d.,]+)([DC])"), // 1,26D (sem R$)
      Pattern.compile("([\\d.,]+)\\s*([DC])"), // 1,26 D (sem R$)
      // Padrões especiais para transferências que podem ter quebra de linha
      Pattern.compile("R\\$\\s*([\\d]{1,3}(?:\\.[\\d]{3})*,[\\d]{2})\\s*D"), // R$ 1.135,00D
      Pattern.compile("([\\d]{1,3}(?:\\.[\\d]{3})*,[\\d]{2})\\s*D"), // 1.135,00D
    };

    boolean valueFound = false;
    for (Pattern pattern : valuePatterns) {
      Matcher flexibleMatcher = pattern.matcher(history);
      if (flexibleMatcher.find()) {
        try {
          String valueStr = flexibleMatcher.group(1);
          String typeStr = flexibleMatcher.groupCount() > 1 ? flexibleMatcher.group(2) : "";

          if (valueStr == null
              || valueStr.trim().isEmpty()
              || valueStr.equals(".")
              || valueStr.matches("^[.]+$")) {
            continue;
          }

          if (typeStr.isEmpty()) {
            if (history.toLowerCase().contains("créd")
                || history.toLowerCase().contains("credit")
                || history.toLowerCase().contains("liquidação")
                || history.toLowerCase().contains("ted")) {
              typeStr = "C";
            } else {
              typeStr = "D";
            }
          }

          amount = TransactionUtil.parseAmount(valueStr, typeStr);
          transactionType = TransactionUtil.determineTransactionType(typeStr);

          cleanHistory = history.replaceFirst(Pattern.quote(flexibleMatcher.group()), "").trim();
          valueFound = true;
          break;
        } catch (Exception e) {
          System.out.println(
              "    - Erro ao extrair valor com padrão: "
                  + pattern.pattern()
                  + " - "
                  + e.getMessage());
          continue;
        }
      }
    }

    if (!valueFound) {

    } else {

      if (cleanHistory != null && cleanHistory.length() > 100) {}
    }

    if (cleanHistory != null && cleanHistory.length() > 500) {
      cleanHistory = cleanHistory.substring(0, 497) + "...";
    }

    Category category =
        TransactionUtil.determineCategory(
            cleanHistory.toLowerCase(), transactionType == TransactionType.CREDITO ? "C" : "D");

    Transaction transaction =
        transactionMapper.toTransaction(
            statement,
            "",
            cleanHistory,
            amount,
            category,
            transactionType,
            document,
            "",
            "",
            date.toString());

    String hash =
        TransactionUtil.generateTransactionHash(
            date, document != null ? document : "", amount, cleanHistory);
    transaction.setHash(hash);

    return transaction;
  }

  private String cleanDescription(String description) {

    String cleaned = description.replaceAll("R\\$\\s*[\\d.,]+[DC]", "").trim();

    if (cleaned != null && cleaned.length() > 100) {}

    return cleaned;
  }
}
