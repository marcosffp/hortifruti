# com.hortifruti.sl.hortifruti.service.invoice.tax.icms

Gera o relatório de "Registro de Apuração de ICMS" em PDF, a partir dos dados fiscais das NF-e
emitidas no período.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `IcmsPdfGenerator.java` | `@Component` | Desenha o PDF do relatório (cabeçalho com firma/IE/CNPJ, tabela por CFOP com valores contábeis/base de cálculo/imposto debitado, subtotal e total geral, legenda), com paginação automática. |
| `IcmsReport.java` | `@Component` | Fachada que encadeia `ImcsReportCalculator` + `IcmsPdfGenerator` para produzir o PDF final. |
| `ImcsReportCalculator.java` | `@Component` | Agrega os `CombinedScore` com NF emitida no período, extrai detalhes fiscais de cada NF via `InvoiceQuery` e soma valores/agrupa por CFOP; erros por nota são logados e ignorados individualmente (não interrompem o relatório). |
