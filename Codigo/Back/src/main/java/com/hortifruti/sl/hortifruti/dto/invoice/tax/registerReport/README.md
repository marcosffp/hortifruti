# com.hortifruti.sl.hortifruti.dto.invoice.tax.registerReport

DTO usado no relatório de registro de notas fiscais (relatório fiscal de saída/entrada por espécie e
alíquota), com campos mapeados no formato esperado por esse relatório.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `InvoiceSummaryDetails.java` | record | Linha de resumo de nota fiscal para o relatório de registro: espécie, série, dia, UF, valor, CFOP predominante e alíquota. Campos mapeados via `@JsonProperty` (`especie`, `serie`, `dia`, `uf`, `valor`, `predominante`, `aliquota`). |
