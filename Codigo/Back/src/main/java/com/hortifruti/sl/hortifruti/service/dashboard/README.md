# com.hortifruti.sl.hortifruti.service.dashboard

Agrega dados financeiros (`finance.Transaction`) e comerciais (`purchase.CombinedScore`/`GroupedProduct`) em métricas prontas para o painel administrativo: totais de receita/custo/margem, fluxo de caixa mensal, distribuição por categoria, ranking de despesas e ranking de produtos mais vendidos.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `DashboardService.java` | `@Service` | Monta o `DashboardResponse` tipado consolidado do dashboard (`getDashboardData`): receita/custo/margem de lucro no período, receita por forma de venda (cartão/PIX), fluxo de caixa por mês (receitas x despesas), percentual/valor por categoria de transação, ranking de categorias de despesa em um mês específico, fluxo de vendas semanal por `confirmedAt` de `CombinedScore`, e top 10 produtos por quantidade/valor vendido — transações e agrupamentos confirmados do período são buscados uma única vez por chamada e reaproveitados entre os cálculos, em vez de repetir a mesma query. |
