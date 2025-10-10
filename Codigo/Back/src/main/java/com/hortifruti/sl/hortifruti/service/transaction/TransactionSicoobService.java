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

    // Primeiro filtrar as transações novas
    List<Transaction> newTransactions =
        TransactionUtil.filterNewTransactions(transactions, transactionRepository);

    // Verificar se há transações para salvar
    if (newTransactions.isEmpty()) {
      return new ArrayList<>();
    }

    // Tentar salvar em lote primeiro, se falhar, salvar individualmente
    try {
      List<Transaction> savedTransactions = transactionRepository.saveAll(newTransactions);
      return savedTransactions;
    } catch (DataIntegrityViolationException e) {
      // Se houver erro de duplicata, salvar uma por uma e ignorar duplicatas
      return saveTransactionsIndividually(newTransactions);
    }
  }

  private List<Transaction> saveTransactionsIndividually(List<Transaction> transactions) {
    List<Transaction> savedTransactions = new ArrayList<>();

    for (Transaction transaction : transactions) {
      try {
        Transaction saved = transactionRepository.save(transaction);
        savedTransactions.add(saved);
      } catch (DataIntegrityViolationException duplicateException) {
        // Ignora transações duplicadas e continua com a próxima
        System.out.println("Transação duplicada ignorada durante save: " + transaction.getHash());
      } catch (Exception e) {
        // Log outros erros mas continua processando
        System.err.println("Erro ao salvar transação: " + e.getMessage());
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

      // Ignora linhas irrelevantes
      if (line.isBlank()
          || line.contains("SALDO DO DIA")
          || line.contains("SALDO ANTERIOR")
          || line.matches(
              "\\d{2}/\\d{2}/\\d{2}, \\d{2}:\\d{2}") // Ignora linhas com formato de data e hora
          || line.contains(
              "Sicoob | Internet Banking")) { // Ignora linhas com "Sicoob | Internet Banking"
        continue;
      }

      // Detecta data
      Matcher dateMatcher = DATE_PATTERN.matcher(line);
      if (dateMatcher.find()) {
        if (currentDate != null && historyBuffer.length() > 0) {
          Transaction transaction =
              createTransaction(currentDate, document, historyBuffer.toString().trim(), statement);
          // Só adiciona se o hash foi gerado corretamente
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

      // Detecta valor
      Matcher valueMatcher = VALUE_PATTERN.matcher(line);
      if (valueMatcher.find() && currentDate != null) {
        Transaction transaction =
            createTransactionFromMatcher(
                currentDate, document, historyBuffer.toString().trim(), valueMatcher, statement);
        // Só adiciona se o hash foi gerado corretamente
        if (transaction.getHash() != null && !transaction.getHash().isEmpty()) {
          transactions.add(transaction);
        }
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
      Transaction transaction =
          createTransaction(currentDate, document, historyBuffer.toString().trim(), statement);
      // Só adiciona se o hash foi gerado corretamente
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

    // Garantir que o hash seja gerado corretamente
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

    // Garantir que o hash seja gerado corretamente
    String hash =
        TransactionUtil.generateTransactionHash(
            date, document != null ? document : "", BigDecimal.ZERO, history);
    transaction.setHash(hash);

    return transaction;
  }

  private String cleanDescription(String description) {
    // Remove padrões de valores como "R$ 3,02D" ou "R$ 3,02C"
    return description.replaceAll("R\\$\\s*[\\d.,]+[DC]", "").trim();
  }
}
