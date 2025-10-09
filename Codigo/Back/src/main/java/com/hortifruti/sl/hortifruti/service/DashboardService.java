package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.repository.TransactionRepository;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;

    /**
     * Método principal público que retorna um objeto com todas as informações do dashboard.
     */
    public Map<String, Object> getDashboardData(LocalDate startDate, LocalDate endDate, Month month, int year) {
        Map<String, Object> dashboardData = new HashMap<>();

        // Divisória 1: Totais de receita, custo e margem de lucro
        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("TotalReceita", calculateTotalRevenue(startDate, endDate));
        totals.put("TotalCusto", calculateTotalCost(startDate, endDate));
        totals.put("MargemLucro", calculateProfitMarginPercentage(startDate, endDate));
        dashboardData.put("Totais", totals);

        // Divisória 2: Receitas por tipo de venda
        Map<String, BigDecimal> salesRevenue = new HashMap<>();
        salesRevenue.put("VendasCartao", calculateCardSalesRevenue(startDate, endDate));
        salesRevenue.put("VendasPix", calculatePixSalesRevenue(startDate, endDate));
        dashboardData.put("ReceitasPorTipo", salesRevenue);

        // Divisória 3: Fluxo de caixa por mês
        dashboardData.put("FluxoDeCaixa", getCashFlowData(startDate, endDate));

        // Divisória 4: Porcentagem por categoria
        dashboardData.put("PorcentagemPorCategoria", getCategoryPercentageData(startDate, endDate));

        // Divisória 5: Ranking de categorias de gastos
        dashboardData.put("RankingCategoriasGastos", getExpenseCategoryRanking(month, year));

        return dashboardData;
    }

    // Métodos privados auxiliares

private BigDecimal calculateTotalByFilter(LocalDate startDate, LocalDate endDate, TransactionType type, Category category) {
    return transactionRepository.findTransactionsByDateRange(startDate, endDate).stream()
            .filter(transaction -> (type == null || transaction.getTransactionType() == type) &&
                                   (category == null || transaction.getCategory() == category))
            .map(transaction -> {
                BigDecimal amount = transaction.getAmount();
                // garante que custos (DEBITO) não fiquem negativos
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
            return BigDecimal.ZERO; // Evita divisão por zero
        }

        return calculatePercentage(totalRevenue.subtract(totalCost), totalRevenue);
    }

    private BigDecimal calculatePercentage(BigDecimal part, BigDecimal total) {
        return part.divide(total, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private Map<Month, Map<String, BigDecimal>> getCashFlowData(LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findTransactionsByDateRange(startDate, endDate);

        return transactions.stream().collect(Collectors.groupingBy(
                transaction -> transaction.getTransactionDate().getMonth(),
                Collectors.groupingBy(
                        transaction -> transaction.getTransactionType() == TransactionType.CREDITO ? "Receitas" : "Despesas",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                )
        ));
    }

    private Map<Category, Map<String, BigDecimal>> getCategoryPercentageData(LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findTransactionsByDateRange(startDate, endDate);

        Map<Category, BigDecimal> categoryTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().abs(), BigDecimal::add)
                ));

        BigDecimal totalAmount = categoryTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return categoryTotals.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    BigDecimal percentage = calculatePercentage(entry.getValue(), totalAmount);
                    Map<String, BigDecimal> data = new HashMap<>();
                    data.put("Porcentagem", percentage);
                    data.put("Valor", entry.getValue());
                    return data;
                }
        ));
    }

    private List<Map<String, Object>> getExpenseCategoryRanking(Month month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Category> expenseCategories = Arrays.asList(
                Category.SERVICOS_BANCARIOS, Category.FORNECEDOR, Category.FAMÍLIA,
                Category.FUNCIONARIO, Category.SERVICOS_TELEFONICOS, Category.CEMIG,
                Category.COPASA, Category.FISCAL, Category.IMPOSTOS
        );

        Map<Category, BigDecimal> categoryCosts = transactionRepository.findTransactionsByDateRange(startDate, endDate).stream()
                .filter(transaction -> expenseCategories.contains(transaction.getCategory()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().abs(), BigDecimal::add)
                ));

        List<Map<String, Object>> ranking = categoryCosts.entrySet().stream()
                .sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed())
                .map(entry -> {
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
}