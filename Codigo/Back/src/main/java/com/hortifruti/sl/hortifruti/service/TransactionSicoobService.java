package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.exception.TransactionException;
import com.hortifruti.sl.hortifruti.mapper.TransactionMapper;
import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.repository.TransactionRepository;
import com.hortifruti.sl.hortifruti.util.TransactionUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class TransactionSicoobService {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{2}/\\d{2})");
  private static final Pattern VALUE_PATTERN = Pattern.compile("R\\$\\s*([\\d.,]+)([DC])");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public List<Transaction> importStatement(MultipartFile file) throws IOException {
    String text = extractPdf(file);
    List<Transaction> transactions = parseSicoob(text);

    // Gera os hashes das transações e consulta os existentes em uma única operação
    Map<String, Transaction> transactionMap =
        transactions.stream()
            .collect(
                Collectors.toMap(
                    tx ->
                        TransactionUtil.generateTransactionHash(
                            tx.getTransactionDate(),
                            tx.getDocument(),
                            tx.getAmount(),
                            tx.getHistory()),
                    tx -> tx));

    Set<String> existingHashes = transactionRepository.findHashes(transactionMap.keySet());
    List<Transaction> newTransactions =
        transactionMap.entrySet().stream()
            .filter(entry -> !existingHashes.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    return transactionRepository.saveAll(newTransactions);
  }

  private String extractPdf(MultipartFile file) throws IOException {
    try (PDDocument document = PDDocument.load(file.getInputStream())) {
      return new PDFTextStripper().getText(document);
    }
  }

  private List<Transaction> parseSicoob(String text) {
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
              createTransaction(currentDate, document, historyBuffer.toString().trim()));
          historyBuffer.setLength(0);
        }

        String[] parts = line.split("\\s+", 3);
        currentDate = parseDate(parts[0]);
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
                currentDate, document, historyBuffer.toString().trim(), valueMatcher));
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
      transactions.add(createTransaction(currentDate, document, historyBuffer.toString().trim()));
    }

    return transactions;
  }

  private LocalDate parseDate(String datePart) {
    datePart = datePart.replaceAll("[^0-9/]", "");
    if (datePart.length() == 5) {
      int year = LocalDate.now().getYear();
      return LocalDate.parse(datePart + "/" + year, DATE_FORMATTER);
    } else if (datePart.length() == 10) {
      return LocalDate.parse(datePart, DATE_FORMATTER);
    } else {
      throw new TransactionException("Formato de data inválido: " + datePart);
    }
  }

  private Transaction createTransactionFromMatcher(
      LocalDate date, String document, String history, Matcher valueMatcher) {
    BigDecimal amount = new BigDecimal(valueMatcher.group(1).replace(".", "").replace(",", "."));
    String type = valueMatcher.group(2);

    TransactionType transactionType =
        "D".equalsIgnoreCase(type) ? TransactionType.DEBITO : TransactionType.CREDITO;
    if ("D".equalsIgnoreCase(type)) {
      amount = amount.negate();
    }

    Category category = determineCategory(history.toLowerCase(), type);

    return transactionMapper.toTransaction(
        date.toString(), document, history, amount, "SICOOB", transactionType, category);
  }

  private Transaction createTransaction(LocalDate date, String document, String history) {
    Matcher valueMatcher = VALUE_PATTERN.matcher(history);
    if (valueMatcher.find()) {
      return createTransactionFromMatcher(date, document, history, valueMatcher);
    }

    return transactionMapper.toTransaction(
        date.toString(),
        document,
        history,
        BigDecimal.ZERO,
        "SICOOB",
        TransactionType.DEBITO,
        Category.FORNECEDOR);
  }

  private Category determineCategory(String historyLower, String balanceType) {
    return TransactionUtil.CATEGORY_KEYWORDS.entrySet().stream()
        .filter(entry -> historyLower.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseGet(
            () -> "D".equalsIgnoreCase(balanceType) ? Category.FORNECEDOR : Category.VENDAS_PIX);
  }
}
