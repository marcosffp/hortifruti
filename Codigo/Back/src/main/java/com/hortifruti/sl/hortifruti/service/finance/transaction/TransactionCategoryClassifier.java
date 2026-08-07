package com.hortifruti.sl.hortifruti.service.finance.transaction;

import com.hortifruti.sl.hortifruti.model.finance.Category;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Classifica automaticamente a categoria contábil de uma transação bancária a partir de palavras-
 * chave no histórico do lançamento. Nomes de funcionários e da família proprietária vêm de
 * configuração ({@code transaction.category.employee-names}/{@code transaction.category.family-
 * names}), não hardcoded no código — trocar um funcionário na lista não exige deploy, só reiniciar
 * com a variável de ambiente atualizada. O mapa é construído uma única vez (não a cada chamada).
 */
@Component
public class TransactionCategoryClassifier {

  private final Map<String, Category> categoryKeywords;

  public TransactionCategoryClassifier(
      @Value("#{'${transaction.category.employee-names}'.split(',')}") List<String> employeeNames,
      @Value("#{'${transaction.category.family-names}'.split(',')}") List<String> familyNames) {
    this.categoryKeywords = buildCategoryKeywords(employeeNames, familyNames);
  }

  private static Map<String, Category> buildCategoryKeywords(
      List<String> employeeNames, List<String> familyNames) {
    Map<String, Category> map = new LinkedHashMap<>();

    map.put("pronampe", Category.SERVICOS_BANCARIOS);
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
    map.put("seg créd proteg", Category.SERVICOS_BANCARIOS);
    map.put("seguro cred prot", Category.SERVICOS_BANCARIOS);
    map.put("devolução", Category.SERVICOS_BANCARIOS);
    map.put("devolucao", Category.SERVICOS_BANCARIOS);
    map.put("estorno", Category.SERVICOS_BANCARIOS);

    map.put("cielo", Category.VENDAS_CARTAO);
    map.put("alelo", Category.VENDAS_CARTAO);
    map.put("hortifruti", Category.VENDAS_CARTAO);
    map.put("pluxee", Category.VENDAS_CARTAO);
    map.put("ted-crédito", Category.VENDAS_CARTAO);
    map.put("cr compras", Category.VENDAS_CARTAO);
    map.put("cr anteci", Category.VENDAS_CARTAO);

    putNames(map, employeeNames, Category.FUNCIONARIO);
    putNames(map, familyNames, Category.FAMÍLIA);

    map.put("claro", Category.SERVICOS_TELEFONICOS);
    map.put("vivo", Category.SERVICOS_TELEFONICOS);

    map.put("cemig", Category.CEMIG);

    map.put("cia de saneamento de mg", Category.COPASA);

    map.put("codigo de barras", Category.IMPOSTOS);
    map.put("rfb-darf codigo de barras", Category.IMPOSTOS);
    map.put("das - simples nacional", Category.IMPOSTOS);
    map.put("cef matriz", Category.IMPOSTOS);

    map.put("singular", Category.FISCAL);
    map.put("next", Category.FISCAL);
    return map;
  }

  private static void putNames(Map<String, Category> map, List<String> names, Category category) {
    for (String name : names) {
      String trimmed = name.trim().toLowerCase();
      if (!trimmed.isEmpty()) {
        map.put(trimmed, category);
      }
    }
  }

  public Category determineCategory(String historyLower, String balanceType) {
    if (historyLower.contains("recebimento fornecedor")) {
      return Category.VENDAS_CARTAO;
    }

    return categoryKeywords.entrySet().stream()
        .filter(entry -> historyLower.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseGet(
            () -> "D".equalsIgnoreCase(balanceType) ? Category.FORNECEDOR : Category.VENDAS_PIX);
  }

  public String categoryLabel(Category category) {
    return switch (category) {
      case VENDAS_CARTAO -> "Vendas Cartão";
      case VENDAS_PIX -> "Vendas PIX";
      case SERVICOS_BANCARIOS -> "Serviços Bancários";
      case FORNECEDOR -> "Fornecedor";
      case FAMÍLIA -> "Família";
      case FUNCIONARIO -> "Funcionário";
      case SERVICOS_TELEFONICOS -> "Serviços Telefônicos";
      case CEMIG -> "Cemig";
      case COPASA -> "Copasa";
      case FISCAL -> "Fiscal";
      case IMPOSTOS -> "Impostos";
    };
  }
}
