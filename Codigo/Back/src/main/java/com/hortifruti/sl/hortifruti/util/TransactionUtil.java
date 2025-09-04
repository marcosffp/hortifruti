package com.hortifruti.sl.hortifruti.util;

import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TransactionUtil {
  public static final Map<String, Category> CATEGORY_KEYWORDS = initCategoryKeywords();

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

    // Vendas Cartão
    map.put("cielo", Category.VENDAS_CARTAO);
    map.put("alelo", Category.VENDAS_CARTAO);
    map.put("hortifruti", Category.VENDAS_CARTAO);
    map.put("pluxeee", Category.VENDAS_CARTAO);
    map.put("cr compras", Category.VENDAS_CARTAO);
    map.put("cr anteci", Category.VENDAS_CARTAO);

    // Funcionário
    map.put("alexandre c", Category.FUNCIONARIO);
    map.put("marlucia natania vieira", Category.FUNCIONARIO);
    map.put("amanda gabriele da silva", Category.FUNCIONARIO);
    map.put("anderson cosme de souza", Category.FUNCIONARIO);

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
}
