package com.hortifruti.sl.hortifruti.service;

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
public class TransactionBBService {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  private static final Pattern TRANSACTION_PATTERN =
      Pattern.compile(
          "^(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(.+?)\\s+([\\d.,]+)\\s+([CD])(?:\\s+([\\d.,]+)\\s+([CD]))?$");
  private static final Pattern DETAIL_PATTERN =
      Pattern.compile("^\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}\\s+.+");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public List<Transaction> importStatement(MultipartFile file) throws IOException {
    String text = extractPdf(file);

    List<Transaction> transactions = parseBancoBrasil(text);

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

    // Filtra as transações que ainda não existem no banco de dados
    List<Transaction> newTransactions =
        transactionMap.entrySet().stream()
            .filter(entry -> !existingHashes.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

    // Salva apenas as novas transações
    return transactionRepository.saveAll(newTransactions);
  }

  private String extractPdf(MultipartFile file) throws IOException {
    try (PDDocument document = PDDocument.load(file.getInputStream())) {
      PDFTextStripper pdfStripper = new PDFTextStripper();
      return pdfStripper.getText(document);
    }
  }

  private List<Transaction> parseBancoBrasil(String text) {
    List<Transaction> transactions = new ArrayList<>();
    String[] lines = text.split("\n");

    for (int i = 0; i < lines.length; i++) {
      String currentLine = lines[i].trim();
      Matcher matcher = TRANSACTION_PATTERN.matcher(currentLine);

      if (matcher.find()) {
        String description = matcher.group(5).trim();

        // Verifica se a próxima linha contém detalhes adicionais
        if (i + 1 < lines.length && DETAIL_PATTERN.matcher(lines[i + 1].trim()).matches()) {
          description += " - " + lines[++i].trim();
        }

        // Cria a primeira transação
        transactions.add(
            createTransaction(matcher, description, matcher.group(6), matcher.group(7)));

        // Cria a segunda transação, se existir
        if (matcher.group(8) != null && matcher.group(9) != null) {
          transactions.add(
              createTransaction(matcher, description, matcher.group(8), matcher.group(9)));
        }
      }
    }

    return transactions;
  }

  private Transaction createTransaction(
      Matcher matcher, String description, String value, String balanceType) {
    LocalDate transactionDate = LocalDate.parse(matcher.group(1), DATE_FORMATTER);
    String document = matcher.group(4);
    BigDecimal amount = new BigDecimal(value.replace(".", "").replace(",", "."));
    TransactionType transactionType =
        "D".equalsIgnoreCase(balanceType) ? TransactionType.DEBITO : TransactionType.CREDITO;

    if ("D".equalsIgnoreCase(balanceType)) {
      amount = amount.negate();
    }

    Category category = determineCategory(description.toLowerCase(), balanceType);

    return transactionMapper.toTransaction(
        transactionDate.toString(), document, description, amount, "BB", transactionType, category);
  }

  private Category determineCategory(String historyLower, String balanceType) {
    // Busca a categoria diretamente no mapa
    return TransactionUtil.CATEGORY_KEYWORDS.entrySet().stream()
        .filter(entry -> historyLower.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseGet(
            () -> {
              // Categorias padrão baseadas no tipo de transação
              if ("D".equalsIgnoreCase(balanceType)) {
                return Category.FORNECEDOR;
              } else if ("C".equalsIgnoreCase(balanceType)) {
                return Category.VENDAS_PIX;
              }
              return null;
            });
  }
}
