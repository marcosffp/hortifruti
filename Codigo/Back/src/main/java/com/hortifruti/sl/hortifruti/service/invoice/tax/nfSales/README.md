# com.hortifruti.sl.hortifruti.service.invoice.tax.nfSales

Coleta os XMLs das NF-e de saída emitidas no período e os empacota em um ZIP, para entrega junto
aos demais relatórios fiscais mensais.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `NfSalesCalculator.java` | `@Service` | Busca os XMLs já persistidos localmente via `FiscalNoteXmlStorageService` (não faz chamada per-nota à Focus NFe, evitando ZIPs incompletos por timeout/rate limit) e grava cada um em arquivo temporário. |
| `NfSalesReport.java` | `@Service` | Fachada que encadeia `NfSalesCalculator` + `NfSalesZipGenerator` para gerar o ZIP final ou apenas listar os arquivos XML do período. |
| `NfSalesZipGenerator.java` | `@Service` | Copia os arquivos XML para uma pasta temporária nomeada por período (`NF_Sales_MM-yyyy_to_MM-yyyy`) e compacta em ZIP, limpando os arquivos temporários ao final. |
