# com.hortifruti.sl.hortifruti.dto.invoice.tax

DTOs de detalhamento tributário de uma nota fiscal específica, usados para exibir/consultar os
valores de impostos (ICMS) de uma nota e de seus itens, mapeando diretamente campos retornados pela
Focus NFe.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `InvoiceTaxDetails.java` | record | Detalhes tributários de uma nota fiscal: status, número, data de emissão, valor dos produtos, valor total, base de cálculo e valor total do ICMS, lista de itens tributáveis (`ItemTaxDetails`) e referência (`ref`). Campos mapeados via `@JsonProperty` no formato da Focus NFe. |
| `ItemTaxDetails.java` | record | Detalhes tributários de um item da nota: CFOP, valor bruto e situação tributária do ICMS. |

## Subpacotes

- `icms/` — relatório consolidado de ICMS sobre vendas (agregado por CFOP).
- `registerReport/` — detalhes de resumo de notas para relatório de registro fiscal.
- `sales/` — detalhes de resumo de vendas para relatório.
