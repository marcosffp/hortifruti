package com.hortifruti.sl.hortifruti.service.transaction;

import com.hortifruti.sl.hortifruti.mapper.TransactionMapper;
import com.hortifruti.sl.hortifruti.model.Statement;
import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.repository.TransactionRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class TransactionSicoobService {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{2}/\\d{2})");
  private static final Pattern VALUE_PATTERN = Pattern.compile("R\\$\\s*([\\d.,]+)([DC])");

  public List<Transaction> importStatement(MultipartFile file, Statement statement)
      throws IOException {
    String text = PdfUtil.extractPdfText(file);
    List<Transaction> transactions = parseSicoob(text, statement);
    List<Transaction> newTransactions =
        TransactionUtil.filterNewTransactions(transactions, transactionRepository);
    return transactionRepository.saveAll(newTransactions);
  }

  private List<Transaction> parseSicoob(String text, Statement statement) {
    List<Transaction> transactions = new ArrayList<>();
    String[] lines = text.split("\n");

    LocalDate currentDate = null;
    StringBuilder historyBuffer = new StringBuilder();
    String document = null;

    for (String rawLine : lines) {
      String line = rawLine.trim();

      // Ignora linhas irrelevantes
      if (line.isBlank() || line.contains("SALDO DO DIA") || line.contains("SALDO ANTERIOR")) {
        continue;
      }

      // Detecta data
      Matcher dateMatcher = DATE_PATTERN.matcher(line);
      if (dateMatcher.find()) {
        if (currentDate != null && historyBuffer.length() > 0) {
          transactions.add(
              createTransaction(currentDate, document, historyBuffer.toString().trim(), statement));
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

      // Detecta valor
      Matcher valueMatcher = VALUE_PATTERN.matcher(line);
      if (valueMatcher.find() && currentDate != null) {
        transactions.add(
            createTransactionFromMatcher(
                currentDate, document, historyBuffer.toString().trim(), valueMatcher, statement));
        historyBuffer.setLength(0);
        document = null;
        currentDate = null;
      } else if (currentDate != null) {
        // Continua preenchendo histórico
        historyBuffer.append(line).append(" ");
      }
    }

    // Adiciona última transação, se houver
    if (currentDate != null && historyBuffer.length() > 0) {
      transactions.add(
          createTransaction(currentDate, document, historyBuffer.toString().trim(), statement));
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
    return transactionMapper.toTransaction(
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
  }

  private Transaction createTransaction(
      LocalDate date, String document, String history, Statement statement) {
    Matcher valueMatcher = VALUE_PATTERN.matcher(history);
    if (valueMatcher.find()) {
      return createTransactionFromMatcher(date, document, history, valueMatcher, statement);
    }

    return transactionMapper.toTransaction(
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
  }

  private String cleanDescription(String description) {
    // Remove padrões de valores como "R$ 3,02D" ou "R$ 3,02C"
    return description.replaceAll("R\\$\\s*[\\d.,]+[DC]", "").trim();
  }
}
