package com.hortifruti.sl.hortifruti.util;

import com.hortifruti.sl.hortifruti.exception.TransactionException;
import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.repository.TransactionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TransactionUtil {
  private TransactionUtil() {}

  private static Map<String, Category> initCategoryKeywords() {
    Map<String, Category> map = new HashMap<>();

    // Serviços Bancários
    map.put("giro pronampe", Category.SERVICOS_BANCARIOS);
    map.put("emprestimo", Category.SERVICOS_BANCARIOS);
    map.put("rende fácil", Category.SERVICOS_BANCARIOS);
    map.put("tar. agrupadas", Category.SERVICOS_BANCARIOS);
    map.put("cobrança referente", Category.SERVICOS_BANCARIOS);
    map.put("empresarial visa", Category.SERVICOS_BANCARIOS);
    map.put("tarifa", Category.SERVICOS_BANCARIOS);
    map.put("débito pacote", Category.SERVICOS_BANCARIOS);
    map.put("déb.empréstimo", Category.SERVICOS_BANCARIOS);
    map.put("déb.tit", Category.SERVICOS_BANCARIOS);
    map.put("créd.liquidação", Category.SERVICOS_BANCARIOS);
    map.put("cessão créd liquid princ", Category.SERVICOS_BANCARIOS);

    // Vendas Cartão
    map.put("cielo", Category.VENDAS_CARTAO);
    map.put("alelo", Category.VENDAS_CARTAO);
    map.put("hortifruti", Category.VENDAS_CARTAO);
    map.put("pluxeee", Category.VENDAS_CARTAO);
    map.put("ted-crédito", Category.VENDAS_CARTAO);
    map.put("cr compras", Category.VENDAS_CARTAO);
    map.put("cr anteci", Category.VENDAS_CARTAO);
    map.put("Recebimento Fornecedor", Category.VENDAS_CARTAO);

    // Funcionário
    map.put("alexandre c", Category.FUNCIONARIO);
    map.put("marlucia natania vieira", Category.FUNCIONARIO);
    map.put("amanda gabriele da silva", Category.FUNCIONARIO);
    map.put("anderson cosme de souza", Category.FUNCIONARIO);
    map.put("alexandre conceição dos sa", Category.FUNCIONARIO);

    // Família
    map.put("marcos", Category.FAMÍLIA);

    // Serviços Telefônicos
    map.put("claro", Category.SERVICOS_TELEFONICOS);
    map.put("vivo", Category.SERVICOS_TELEFONICOS);

    // Cemig
    map.put("cemig", Category.CEMIG);

    // Copasa
    map.put("cia de saneamento de mg", Category.COPASA);

    // Impostos
    map.put("codigo de barras", Category.IMPOSTOS);
    map.put("rfb-darf codigo de barras", Category.IMPOSTOS);
    map.put("das - simples nacional", Category.IMPOSTOS);
    map.put("cef matriz", Category.IMPOSTOS);

    // Fiscal
    map.put("singular", Category.FISCAL);
    map.put("next", Category.FISCAL);

    /*
     * // Fornecedor
     * map.put("mercado", Category.FORNECEDOR);
     *
     * map.put("pix", Category.VENDAS_PIX);
     */

    return map;
  }

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
    // Extrai os hashes das transações fornecidas
    List<String> hashes =
        transactions.stream()
            .map(Transaction::getHash) // Usa o hash já gerado na entidade
            .collect(Collectors.toList());

    // Consulta os hashes existentes no banco
    Set<String> existingHashes = transactionRepository.findHashes(new HashSet<>(hashes));

    // Filtra as transações que ainda não existem no banco, mantendo a ordem original
    return transactions.stream()
        .filter(tx -> !existingHashes.contains(tx.getHash())) // Usa o hash diretamente
        .collect(Collectors.toList());
  }

  public static Category determineCategory(String historyLower, String balanceType) {
    // Verifica explicitamente "Recebimento Fornecedor"
    if (historyLower.contains("recebimento fornecedor")) {
      return Category.VENDAS_CARTAO;
    }

    // Aplica a lógica padrão com base nas palavras-chave
    return initCategoryKeywords().entrySet().stream()
        .filter(entry -> historyLower.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseGet(
            () -> "D".equalsIgnoreCase(balanceType) ? Category.FORNECEDOR : Category.VENDAS_PIX);
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
