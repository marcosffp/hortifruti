package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class DashboardService {

  private final TransactionRepository transactionRepository;
  private final CombinedScoreRepository combinedScoreRepository;

  @Transactional(readOnly = true)
  public Map<String, Object> getDashboardData(
      LocalDate startDate, LocalDate endDate, Month month, int year) {
    Map<String, Object> dashboardData = new HashMap<>();

    Map<String, BigDecimal> totals = new HashMap<>();
    totals.put("TotalReceita", calculateTotalRevenue(startDate, endDate));
    totals.put("TotalCusto", calculateTotalCost(startDate, endDate));
    totals.put("MargemLucro", calculateProfitMarginPercentage(startDate, endDate));
    dashboardData.put("Totais", totals);

    Map<String, BigDecimal> salesRevenue = new HashMap<>();
    salesRevenue.put("VendasCartao", calculateCardSalesRevenue(startDate, endDate));
    salesRevenue.put("VendasPix", calculatePixSalesRevenue(startDate, endDate));
    dashboardData.put("ReceitasPorTipo", salesRevenue);

    dashboardData.put("FluxoDeCaixa", getCashFlowData(startDate, endDate));

    dashboardData.put("PorcentagemPorCategoria", getCategoryPercentageData(startDate, endDate));

    dashboardData.put("RankingCategoriasGastos", getExpenseCategoryRanking(month, year));

    dashboardData.put("Fluxo de Vendas", getCombinedScoreData(startDate, endDate));

    dashboardData.put("Produtos em Alta", getTopSellingProducts(startDate, endDate));

    dashboardData.put("Top10ProdutosPorQuantidade", getTopProductsByQuantity(startDate, endDate));

    return dashboardData;
  }

  private BigDecimal calculateTotalByFilter(
      LocalDate startDate, LocalDate endDate, TransactionType type, Category category) {
    return transactionRepository.findTransactionsByDateRange(startDate, endDate).stream()
        .filter(
            transaction ->
                (type == null || transaction.getTransactionType() == type)
                    && (category == null || transaction.getCategory() == category))
        .map(
            transaction -> {
              BigDecimal amount = transaction.getAmount();
              if (transaction.getTransactionType() == TransactionType.DEBITO) {
                amount = amount.abs();
              }
              return amount;
            })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateTotalRevenue(LocalDate startDate, LocalDate endDate) {
    return calculateTotalByFilter(startDate, endDate, TransactionType.CREDITO, null);
  }

  private BigDecimal calculateTotalCost(LocalDate startDate, LocalDate endDate) {
    return calculateTotalByFilter(startDate, endDate, TransactionType.DEBITO, null);
  }

  private BigDecimal calculateCardSalesRevenue(LocalDate startDate, LocalDate endDate) {
    return calculateTotalByFilter(startDate, endDate, null, Category.VENDAS_CARTAO);
  }

  private BigDecimal calculatePixSalesRevenue(LocalDate startDate, LocalDate endDate) {
    return calculateTotalByFilter(startDate, endDate, null, Category.VENDAS_PIX);
  }

  private BigDecimal calculateProfitMarginPercentage(LocalDate startDate, LocalDate endDate) {
    BigDecimal totalRevenue = calculateTotalRevenue(startDate, endDate);
    BigDecimal totalCost = calculateTotalCost(startDate, endDate);

    if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    return calculatePercentage(totalRevenue.subtract(totalCost), totalRevenue);
  }

  private BigDecimal calculatePercentage(BigDecimal part, BigDecimal total) {
    return part.divide(total, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
  }

  private Map<Month, Map<String, BigDecimal>> getCashFlowData(
      LocalDate startDate, LocalDate endDate) {
    List<Transaction> transactions =
        transactionRepository.findTransactionsByDateRange(startDate, endDate);

    return transactions.stream()
        .collect(
            Collectors.groupingBy(
                transaction -> transaction.getTransactionDate().getMonth(),
                Collectors.groupingBy(
                    transaction ->
                        transaction.getTransactionType() == TransactionType.CREDITO
                            ? "Receitas"
                            : "Despesas",
                    Collectors.reducing(
                        BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add))));
  }

  private Map<Category, Map<String, BigDecimal>> getCategoryPercentageData(
      LocalDate startDate, LocalDate endDate) {
    List<Transaction> transactions =
        transactionRepository.findTransactionsByDateRange(startDate, endDate);

    Map<Category, BigDecimal> categoryTotals =
        transactions.stream()
            .collect(
                Collectors.groupingBy(
                    Transaction::getCategory,
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

    Map<Category, BigDecimal> absoluteCategoryTotals =
        categoryTotals.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().abs()));

    BigDecimal totalAmount =
        absoluteCategoryTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    return absoluteCategoryTotals.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                  BigDecimal percentage = calculatePercentage(entry.getValue(), totalAmount);
                  Map<String, BigDecimal> data = new HashMap<>();
                  data.put("Porcentagem", percentage);
                  data.put("Valor", entry.getValue());
                  return data;
                }));
  }

  private List<Map<String, Object>> getExpenseCategoryRanking(Month month, int year) {
    LocalDate startDate = LocalDate.of(year, month, 1);
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

    List<Category> expenseCategories =
        Arrays.asList(
            Category.SERVICOS_BANCARIOS,
            Category.FORNECEDOR,
            Category.FAMÍLIA,
            Category.FUNCIONARIO,
            Category.SERVICOS_TELEFONICOS,
            Category.CEMIG,
            Category.COPASA,
            Category.FISCAL,
            Category.IMPOSTOS);

    Map<Category, BigDecimal> categoryCosts =
        transactionRepository.findTransactionsByDateRange(startDate, endDate).stream()
            .filter(transaction -> expenseCategories.contains(transaction.getCategory()))
            .collect(
                Collectors.groupingBy(
                    Transaction::getCategory,
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

    Map<Category, BigDecimal> absoluteCategoryCosts =
        categoryCosts.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().abs()));

    List<Map<String, Object>> ranking =
        absoluteCategoryCosts.entrySet().stream()
            .sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed())
            .map(
                entry -> {
                  Map<String, Object> rankData = new HashMap<>();
                  rankData.put("Categoria", entry.getKey().name());
                  rankData.put("Valor", entry.getValue());
                  return rankData;
                })
            .collect(Collectors.toList());

    for (int i = 0; i < ranking.size(); i++) {
      ranking.get(i).put("Rank", i + 1);
    }

    return ranking;
  }

  /**
   * Retorna o fluxo de vendas agrupado por semana. Utiliza a data de CONFIRMAÇÃO (confirmedAt) ao
   * invés de vencimento (dueDate).
   */
  public Map<String, Object> getCombinedScoreData(LocalDate startDate, LocalDate endDate) {
    Map<String, Object> combinedScoreData = new HashMap<>();

    List<CombinedScore> combinedScores =
        combinedScoreRepository.findAllByOrderByIdDesc(Pageable.unpaged()).stream()
            .filter(
                cs -> {
                  if (cs.getConfirmedAt() == null) {
                    return false;
                  }
                  LocalDate confirmedDate = cs.getConfirmedAt();
                  return !confirmedDate.isBefore(startDate) && !confirmedDate.isAfter(endDate);
                })
            .collect(Collectors.toList());

    Map<Integer, BigDecimal> weeklyScores =
        combinedScores.stream()
            .filter(cs -> cs.getConfirmedAt() != null)
            .collect(
                Collectors.groupingBy(
                    cs -> cs.getConfirmedAt().get(ChronoField.ALIGNED_WEEK_OF_YEAR),
                    Collectors.reducing(
                        BigDecimal.ZERO, CombinedScore::getTotalValue, BigDecimal::add)));

    weeklyScores.forEach(
        (week, totalScore) -> {
          combinedScoreData.put("Semana " + week, totalScore);
        });

    return combinedScoreData;
  }

  /**
   * Retorna os top 10 produtos em alta (ordenados por quantidade e valor). Utiliza a data de
   * CONFIRMAÇÃO (confirmedAt) ao invés de vencimento (dueDate).
   */
  public List<Map<String, Object>> getTopSellingProducts(LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores =
        combinedScoreRepository.findAllByOrderByIdDesc(Pageable.unpaged()).stream()
            .filter(
                cs -> {
                  if (cs.getConfirmedAt() == null) {
                    return false;
                  }
                  LocalDate confirmedDate = cs.getConfirmedAt();
                  return !confirmedDate.isBefore(startDate) && !confirmedDate.isAfter(endDate);
                })
            .collect(Collectors.toList());

    List<GroupedProduct> groupedProducts =
        combinedScores.stream()
            .flatMap(cs -> cs.getGroupedProducts().stream())
            .collect(Collectors.toList());

    Map<String, Map<String, Object>> productData =
        groupedProducts.stream()
            .collect(
                Collectors.groupingBy(
                    GroupedProduct::getCode,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        products -> {
                          Map<String, Object> data = new HashMap<>();
                          data.put("Nome", products.get(0).getName());
                          data.put(
                              "QuantidadeTotal",
                              products.stream()
                                  .map(GroupedProduct::getQuantity)
                                  .reduce(BigDecimal.ZERO, BigDecimal::add));
                          data.put(
                              "ValorTotal",
                              products.stream()
                                  .map(GroupedProduct::getTotalValue)
                                  .reduce(BigDecimal.ZERO, BigDecimal::add));
                          return data;
                        })));

    List<Map<String, Object>> ranking = new ArrayList<>(productData.values());
    ranking.sort(
        (p1, p2) -> {
          int quantityComparison =
              ((BigDecimal) p2.get("QuantidadeTotal"))
                  .compareTo((BigDecimal) p1.get("QuantidadeTotal"));
          if (quantityComparison != 0) {
            return quantityComparison;
          }
          return ((BigDecimal) p2.get("ValorTotal")).compareTo((BigDecimal) p1.get("ValorTotal"));
        });

    return ranking.stream().limit(10).collect(Collectors.toList());
  }

  /**
   * Retorna os top 10 produtos com mais saída de quantidade em um período. Utiliza a data de
   * CONFIRMAÇÃO (confirmedAt) ao invés de vencimento (dueDate).
   */
  public List<Map<String, Object>> getTopProductsByQuantity(
      LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores =
        combinedScoreRepository.findAllByOrderByIdDesc(Pageable.unpaged()).stream()
            .filter(
                cs -> {
                  if (cs.getConfirmedAt() == null) {
                    return false;
                  }
                  LocalDate confirmedDate = cs.getConfirmedAt();
                  return !confirmedDate.isBefore(startDate) && !confirmedDate.isAfter(endDate);
                })
            .collect(Collectors.toList());

    List<GroupedProduct> groupedProducts =
        combinedScores.stream()
            .flatMap(cs -> cs.getGroupedProducts().stream())
            .collect(Collectors.toList());

    Map<String, Map<String, Object>> productData =
        groupedProducts.stream()
            .collect(
                Collectors.groupingBy(
                    GroupedProduct::getCode,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        products -> {
                          Map<String, Object> data = new HashMap<>();
                          data.put("Nome", products.get(0).getName());
                          data.put(
                              "QuantidadeTotal",
                              products.stream()
                                  .map(GroupedProduct::getQuantity)
                                  .reduce(BigDecimal.ZERO, BigDecimal::add));
                          return data;
                        })));

    List<Map<String, Object>> ranking = new ArrayList<>(productData.values());
    ranking.sort(
        (p1, p2) ->
            ((BigDecimal) p2.get("QuantidadeTotal"))
                .compareTo((BigDecimal) p1.get("QuantidadeTotal")));

    return ranking.stream().limit(10).collect(Collectors.toList());
  }
}
