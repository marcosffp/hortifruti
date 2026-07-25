# com.hortifruti.sl.hortifruti.service.invoice.tax.sales

Gera o relatório "Relação de Vendas" em PDF, listando cada NF-e emitida no período com dados do
cliente e valores.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `SalesCalculator.java` | `@Service` | Para cada NF-e emitida no período, monta um `SalesSummaryDetails` (número, modelo fixo "55", data, cliente resolvido via `ClientService`, subtotal/desconto/acréscimo/total); erros por nota são apenas logados. |
| `SalesPdfGenerator.java` | `@Component` | Desenha o PDF com layout de colunas de largura variável (próprio, não usa `PdfReportSupport.drawTableHeader`/`drawTableRow`): Número/Mod/Data/Envio/Cliente/Subtotal/Desconto/Acréscimo/Total, truncando nome do cliente longo. |
| `SalesReport.java` | `@Service` | Fachada que encadeia `SalesCalculator` + `SalesPdfGenerator` para produzir o PDF final. |
