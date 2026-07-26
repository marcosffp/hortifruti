# com.hortifruti.sl.hortifruti.service.invoice.tax.registerReport

Gera o "Livro de Registro de Saídas - RE - Modelo P 2/A" em PDF, exigido para escrituração fiscal
mensal.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `RegisterCalculator.java` | `@Service` | Para cada NF-e emitida no período, monta um `InvoiceSummaryDetails` (espécie, série, dia, UF, valor, CFOP predominante e alíquota); CFOP predominante exige presença em ≥80% dos itens da nota, senão marca "Indefinido"; alíquota fixa por CFOP (5102 → 18%, 5405 → 0%). |
| `RegisterPdfGenerator.java` | `@Component` | Desenha o PDF (cabeçalho firma/CNPJ/período, tabela Espécie/Série/Dia/UF/Valor/Cod.Fiscal/Aliq./Outras, legenda), com paginação automática. |
| `RegisterReport.java` | `@Service` | Fachada que encadeia `RegisterCalculator` + `RegisterPdfGenerator` para produzir o PDF final. |
