package com.hortifruti.sl.hortifruti.util;

import com.hortifruti.sl.hortifruti.exception.finance.TransactionException;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.model.finance.TransactionType;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilitários genéricos de parsing/dedupe de transações bancárias, sem regra de negócio embutida —
 * a classificação por categoria (que depende de configuração e nomes de funcionários) vive em
 * {@link com.hortifruti.sl.hortifruti.service.finance.transaction.TransactionCategoryClassifier}.
 */
public final class TransactionUtil {
  private TransactionUtil() {}

  public static String generateTransactionHash(
      LocalDate date, String document, BigDecimal amount, String history) {
    try {
      String input = date.toString() + document + amount.toString() + history;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Erro ao gerar hash da transação", e);
    }
  }

  public static List<Transaction> filterNewTransactions(
      List<Transaction> transactions, TransactionRepository transactionRepository) {
    List<String> hashes =
        transactions.stream().map(Transaction::getHash).collect(Collectors.toList());

    Set<String> existingHashes = transactionRepository.findHashes(new HashSet<>(hashes));

    return transactions.stream()
        .filter(tx -> !existingHashes.contains(tx.getHash()))
        .collect(Collectors.toList());
  }

  public static BigDecimal parseAmount(String value, String type) {
    BigDecimal amount = new BigDecimal(value.replace(".", "").replace(",", "."));
    return "D".equalsIgnoreCase(type) ? amount.negate() : amount;
  }

  public static TransactionType determineTransactionType(String type) {
    if ("D".equalsIgnoreCase(type)) {
      return TransactionType.DEBITO;
    }
    return TransactionType.CREDITO;
  }

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public static LocalDate parseDate(String datePart) {
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
}
