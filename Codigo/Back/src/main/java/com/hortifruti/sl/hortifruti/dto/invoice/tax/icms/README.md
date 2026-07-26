# com.hortifruti.sl.hortifruti.dto.invoice.tax.icms

DTO de relatório consolidado de ICMS sobre vendas, agregando valores fiscais de um conjunto de notas
para fins de apuração/relatório contábil.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `IcmsSalesReport.java` | record | Relatório de ICMS sobre vendas: total contábil, total da base de cálculo, total do imposto debitado, total de operações isentas/não tributadas, total de outras operações e um mapa (`valoresPorCfop`) com o valor agregado por código CFOP. |
