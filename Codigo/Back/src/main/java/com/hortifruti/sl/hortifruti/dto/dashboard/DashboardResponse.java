package com.hortifruti.sl.hortifruti.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hortifruti.sl.hortifruti.model.finance.Category;
import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Map;

/**
 * Contrato tipado do payload de {@code GET /dashboard}. As chaves JSON (incluindo as com espaço,
 * ex. {@code "Fluxo de Vendas"}) são mantidas exatamente como o frontend já consome hoje
 * (`Codigo/Front/src/services/dashboardService.ts`) — só o lado do backend deixa de ser um {@code
 * Map<String, Object>} sem contrato, onde um erro de digitação numa chave só aparece em runtime.
 */
public record DashboardResponse(
    @JsonProperty("Totais") Totals totais,
    @JsonProperty("ReceitasPorTipo") SalesRevenue receitasPorTipo,
    @JsonProperty("FluxoDeCaixa") Map<Month, Map<String, BigDecimal>> fluxoDeCaixa,
    @JsonProperty("PorcentagemPorCategoria")
        Map<Category, CategoryPercentage> porcentagemPorCategoria,
    @JsonProperty("RankingCategoriasGastos") List<CategoryRanking> rankingCategoriasGastos,
    @JsonProperty("Fluxo de Vendas") Map<String, BigDecimal> fluxoDeVendas,
    @JsonProperty("Produtos em Alta") List<TopProduct> produtosEmAlta,
    @JsonProperty("Top10ProdutosPorQuantidade")
        List<TopProductByQuantity> top10ProdutosPorQuantidade) {

  public record Totals(
      @JsonProperty("TotalReceita") BigDecimal totalReceita,
      @JsonProperty("TotalCusto") BigDecimal totalCusto,
      @JsonProperty("MargemLucro") BigDecimal margemLucro) {}

  public record SalesRevenue(
      @JsonProperty("VendasCartao") BigDecimal vendasCartao,
      @JsonProperty("VendasPix") BigDecimal vendasPix) {}

  public record CategoryPercentage(
      @JsonProperty("Porcentagem") BigDecimal porcentagem,
      @JsonProperty("Valor") BigDecimal valor) {}

  public record CategoryRanking(
      @JsonProperty("Categoria") String categoria,
      @JsonProperty("Valor") BigDecimal valor,
      @JsonProperty("Rank") int rank) {}

  public record TopProduct(
      @JsonProperty("Nome") String nome,
      @JsonProperty("QuantidadeTotal") BigDecimal quantidadeTotal,
      @JsonProperty("ValorTotal") BigDecimal valorTotal) {}

  public record TopProductByQuantity(
      @JsonProperty("Nome") String nome,
      @JsonProperty("QuantidadeTotal") BigDecimal quantidadeTotal) {}
}
