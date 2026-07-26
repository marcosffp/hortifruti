# com.hortifruti.sl.hortifruti.dto.invoice.tax.sales

DTO usado no relatório de resumo de vendas por nota fiscal, com valores de subtotal, desconto,
acréscimo e total.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `SalesSummaryDetails.java` | record | Linha de resumo de venda para relatório: número da nota, modelo (`mod`), data, envio, cliente, subtotal, desconto, acréscimo e total. Campos mapeados via `@JsonProperty` (`numero`, `mod`, `data`, `envio`, `cliente`, `subtotal`, `desconto`, `acrescimo`, `total`). |
