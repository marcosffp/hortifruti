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
public class TransactionBBService {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  private static final Pattern TRANSACTION_PATTERN =
      Pattern.compile(
          "^(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(.+?)\\s+([\\d.,]+)\\s+([CD])(?:\\s+([\\d.,]+)\\s+([CD]))?$");

  protected List<Transaction> importStatement(MultipartFile file, Statement statement)
      throws IOException {
    String text = PdfUtil.extractPdfText(file);
    List<Transaction> transactions = parseBancoBrasil(text, statement);

    List<Transaction> newTransactions =
        TransactionUtil.filterNewTransactions(transactions, transactionRepository);

    // Após salvar as transações
    List<Transaction> savedTransactions = transactionRepository.saveAll(newTransactions);

    return savedTransactions;
  }

  private List<Transaction> parseBancoBrasil(String text, Statement statement) {
    List<Transaction> transactions = new ArrayList<>();
    String[] lines = text.split("\n");

    for (int i = 0; i < lines.length; i++) {
      String currentLine = lines[i].trim();
      Matcher matcher = TRANSACTION_PATTERN.matcher(currentLine);

      if (matcher.find()) {
        String description = matcher.group(5).trim();
        String nextLineDescription = "";

        // Verifica se a próxima linha contém informações adicionais relevantes
        if (i + 1 < lines.length && !TRANSACTION_PATTERN.matcher(lines[i + 1].trim()).matches()) {
          nextLineDescription = " " + lines[i + 1].trim(); // Concatena a próxima linha
          i++; // Avança para evitar processar a mesma linha novamente
        }

        Transaction transaction =
            createTransaction(
                matcher,
                description + nextLineDescription,
                matcher.group(6),
                matcher.group(7),
                statement);
        transactions.add(transaction);

        if (matcher.group(8) != null && matcher.group(9) != null) {
          Transaction secondTransaction =
              createTransaction(
                  matcher,
                  description + nextLineDescription,
                  matcher.group(8),
                  matcher.group(9),
                  statement);
          transactions.add(secondTransaction);
        }
      }
    }

    return transactions;
  }

  private Transaction createTransaction(
      Matcher matcher, String history, String value, String balanceType, Statement statement) {
    LocalDate transactionDate = TransactionUtil.parseDate(matcher.group(1));
    String codHistory = matcher.group(4); // Captura o codHistory corretamente
    BigDecimal amount = TransactionUtil.parseAmount(value, balanceType);
    TransactionType transactionType = TransactionUtil.determineTransactionType(balanceType);

    // Extrai o número do documento, agência de origem e lote
    String document = extractDocument(history);
    String sourceAgency = extractSourceAgency(matcher.group(2)); // Captura da linha do extrato
    String batch = extractBatch(matcher.group(3)); // Captura da linha do extrato
    history = cleanDescription(history);

    Category category = TransactionUtil.determineCategory(history.toLowerCase(), balanceType);

    Transaction transaction =
        transactionMapper.toTransaction(
            statement,
            codHistory, // Está passando o valor corretamente, mas pode estar sendo ignorado pelo
            // mapper
            history,
            amount,
            category,
            transactionType,
            document,
            sourceAgency,
            batch,
            transactionDate.toString());
    return transaction;
  }

  private String extractDocument(String description) {
    // Caso específico para "BB Rende Fácil"
    if (description.toLowerCase().contains("rende fácil")) {
      // Regex para capturar número após "Rende Fácil"
      Pattern rendeFacilPattern =
          Pattern.compile("rende fácil\\s+(\\d+[.,\\d]*)", Pattern.CASE_INSENSITIVE);
      Matcher matcher = rendeFacilPattern.matcher(description);
      if (matcher.find()) {
        return matcher.group(1).replace(",", "."); // Retorna o número com ponto decimal
      }
      return null; // Caso não encontre o número, retorna null
    }

    // Regex para capturar número do documento em outros casos
    Pattern numeroDocumentoPattern = Pattern.compile("\\d{2,}(\\.\\d{3})+");
    Matcher matcher = numeroDocumentoPattern.matcher(description);
    if (matcher.find()) {
      return matcher.group().replace(",", "."); // Retorna o número com ponto decimal
    }
    return null;
  }

  private String extractSourceAgency(String agenciaOrigemField) {
    // Valida e retorna a agência de origem
    return agenciaOrigemField.matches("\\d{4}") ? agenciaOrigemField : null;
  }

  private String extractBatch(String loteField) {
    // Valida e retorna o lote
    return loteField.matches("\\d{5}") ? loteField : null;
  }

  private String cleanDescription(String description) {
    // Caso específico para "Rende Fácil"
    if (description.toLowerCase().contains("rende fácil")) {
      // Remove o número após "Rende Fácil"
      return description.replaceAll("rende fácil\\s+\\d+[.,\\d]*", "Rende Fácil").trim();
    }

    // Remove outros números do documento
    return description.replaceAll("\\d{2,}(\\.\\d{3})+", "").trim();
  }
}
