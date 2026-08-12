package com.hortifruti.sl.hortifruti.service.dashboard;

import com.hortifruti.sl.hortifruti.dto.dashboard.DashboardResponse;
import com.hortifruti.sl.hortifruti.dto.dashboard.DashboardResponse.CategoryPercentage;
import com.hortifruti.sl.hortifruti.dto.dashboard.DashboardResponse.CategoryRanking;
import com.hortifruti.sl.hortifruti.dto.dashboard.DashboardResponse.SalesRevenue;
import com.hortifruti.sl.hortifruti.dto.dashboard.DashboardResponse.TopProduct;
import com.hortifruti.sl.hortifruti.dto.dashboard.DashboardResponse.TopProductByQuantity;
import com.hortifruti.sl.hortifruti.dto.dashboard.DashboardResponse.Totals;
import com.hortifruti.sl.hortifruti.model.finance.Category;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.model.finance.TransactionType;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import com.hortifruti.sl.hortifruti.service.finance.transaction.TransactionProcessingService;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class DashboardService {

  private final TransactionProcessingService transactionProcessingService;
  private final CombinedScoreService combinedScoreService;

  @Transactional(readOnly = true)
  public DashboardResponse getDashboardData(
      LocalDate startDate, LocalDate endDate, Month month, int year) {
    // Buscados uma única vez e reaproveitados por todos os cálculos abaixo — antes, cada cálculo
    // repetia a mesma query de Transaction/CombinedScore pro mesmo intervalo de datas.
    List<Transaction> transactions =
        transactionProcessingService.findTransactionsByDateRange(startDate, endDate);
    List<CombinedScore> combinedScores =
        combinedScoreService.findAllConfirmedBetween(startDate, endDate);

    BigDecimal totalRevenue = calculateTotalRevenue(transactions);
    BigDecimal totalCost = calculateTotalCost(transactions);

    Totals totais =
        new Totals(
            totalRevenue, totalCost, calculateProfitMarginPercentage(totalRevenue, totalCost));

    SalesRevenue receitasPorTipo =
        new SalesRevenue(
            calculateCardSalesRevenue(transactions), calculatePixSalesRevenue(transactions));

    return new DashboardResponse(
        totais,
        receitasPorTipo,
        getCashFlowData(transactions),
        getCategoryPercentageData(transactions),
        getExpenseCategoryRanking(month, year),
        getCombinedScoreData(combinedScores),
        getTopSellingProducts(combinedScores),
        getTopProductsByQuantity(combinedScores));
  }

  private BigDecimal calculateTotalByFilter(
      List<Transaction> transactions, TransactionType type, Category category) {
    return transactions.stream()
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

  private BigDecimal calculateTotalRevenue(List<Transaction> transactions) {
    return calculateTotalByFilter(transactions, TransactionType.CREDITO, null);
  }

  private BigDecimal calculateTotalCost(List<Transaction> transactions) {
    return calculateTotalByFilter(transactions, TransactionType.DEBITO, null);
  }

  private BigDecimal calculateCardSalesRevenue(List<Transaction> transactions) {
    return calculateTotalByFilter(transactions, null, Category.VENDAS_CARTAO);
  }

  private BigDecimal calculatePixSalesRevenue(List<Transaction> transactions) {
    return calculateTotalByFilter(transactions, null, Category.VENDAS_PIX);
  }

  private BigDecimal calculateProfitMarginPercentage(
      BigDecimal totalRevenue, BigDecimal totalCost) {
    if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    return calculatePercentage(totalRevenue.subtract(totalCost), totalRevenue);
  }

  private BigDecimal calculatePercentage(BigDecimal part, BigDecimal total) {
    return part.divide(total, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
  }

  private Map<Month, Map<String, BigDecimal>> getCashFlowData(List<Transaction> transactions) {
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

  private Map<Category, CategoryPercentage> getCategoryPercentageData(
      List<Transaction> transactions) {
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
                entry ->
                    new CategoryPercentage(
                        calculatePercentage(entry.getValue(), totalAmount), entry.getValue())));
  }

  private List<CategoryRanking> getExpenseCategoryRanking(Month month, int year) {
    LocalDate startDate = LocalDate.of(year, month, 1);
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

    List<Category> expenseCategories =
        Arrays.asList(
            Category.SERVICOS_BANCARIOS,
            Category.FORNECEDOR,
            Category.FAMILIA,
            Category.FUNCIONARIO,
            Category.SERVICOS_TELEFONICOS,
            Category.CEMIG,
            Category.COPASA,
            Category.FISCAL,
            Category.IMPOSTOS);

    Map<Category, BigDecimal> categoryCosts =
        transactionProcessingService.findTransactionsByDateRange(startDate, endDate).stream()
            .filter(transaction -> expenseCategories.contains(transaction.getCategory()))
            .collect(
                Collectors.groupingBy(
                    Transaction::getCategory,
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

    Map<Category, BigDecimal> absoluteCategoryCosts =
        categoryCosts.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().abs()));

    List<Map.Entry<Category, BigDecimal>> sorted =
        absoluteCategoryCosts.entrySet().stream()
            .sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed())
            .toList();

    List<CategoryRanking> ranking = new ArrayList<>();
    for (int i = 0; i < sorted.size(); i++) {
      Map.Entry<Category, BigDecimal> entry = sorted.get(i);
      ranking.add(new CategoryRanking(entry.getKey().name(), entry.getValue(), i + 1));
    }

    return ranking;
  }

  /**
   * Retorna o fluxo de vendas agrupado por semana. Utiliza a data de CONFIRMAÇÃO (confirmedAt) ao
   * invés de vencimento (dueDate).
   */
  private Map<String, BigDecimal> getCombinedScoreData(List<CombinedScore> combinedScores) {
    Map<Integer, BigDecimal> weeklyScores =
        combinedScores.stream()
            .collect(
                Collectors.groupingBy(
                    cs -> cs.getConfirmedAt().get(ChronoField.ALIGNED_WEEK_OF_YEAR),
                    Collectors.reducing(
                        BigDecimal.ZERO, CombinedScore::getTotalValue, BigDecimal::add)));

    Map<String, BigDecimal> combinedScoreData = new HashMap<>();
    weeklyScores.forEach((week, totalScore) -> combinedScoreData.put("Semana " + week, totalScore));
    return combinedScoreData;
  }

  private List<GroupedProduct> flattenGroupedProducts(List<CombinedScore> combinedScores) {
    return combinedScores.stream()
        .flatMap(cs -> cs.getGroupedProducts().stream())
        .collect(Collectors.toList());
  }

  /**
   * Retorna os top 10 produtos em alta (ordenados por quantidade e valor). Utiliza a data de
   * CONFIRMAÇÃO (confirmedAt) ao invés de vencimento (dueDate).
   */
  private List<TopProduct> getTopSellingProducts(List<CombinedScore> combinedScores) {
    List<GroupedProduct> groupedProducts = flattenGroupedProducts(combinedScores);

    Map<String, TopProduct> productData =
        groupedProducts.stream()
            .collect(
                Collectors.groupingBy(
                    GroupedProduct::getCode,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        products ->
                            new TopProduct(
                                products.get(0).getName(),
                                products.stream()
                                    .map(GroupedProduct::getQuantity)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add),
                                products.stream()
                                    .map(GroupedProduct::getTotalValue)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)))));

    List<TopProduct> ranking = new ArrayList<>(productData.values());
    ranking.sort(
        (p1, p2) -> {
          int quantityComparison = p2.quantidadeTotal().compareTo(p1.quantidadeTotal());
          if (quantityComparison != 0) {
            return quantityComparison;
          }
          return p2.valorTotal().compareTo(p1.valorTotal());
        });

    return ranking.stream().limit(10).collect(Collectors.toList());
  }

  /**
   * Retorna os top 10 produtos com mais saída de quantidade em um período. Utiliza a data de
   * CONFIRMAÇÃO (confirmedAt) ao invés de vencimento (dueDate).
   */
  private List<TopProductByQuantity> getTopProductsByQuantity(List<CombinedScore> combinedScores) {
    List<GroupedProduct> groupedProducts = flattenGroupedProducts(combinedScores);

    Map<String, TopProductByQuantity> productData =
        groupedProducts.stream()
            .collect(
                Collectors.groupingBy(
                    GroupedProduct::getCode,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        products ->
                            new TopProductByQuantity(
                                products.get(0).getName(),
                                products.stream()
                                    .map(GroupedProduct::getQuantity)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)))));

    List<TopProductByQuantity> ranking = new ArrayList<>(productData.values());
    ranking.sort((p1, p2) -> p2.quantidadeTotal().compareTo(p1.quantidadeTotal()));

    return ranking.stream().limit(10).collect(Collectors.toList());
  }
}
