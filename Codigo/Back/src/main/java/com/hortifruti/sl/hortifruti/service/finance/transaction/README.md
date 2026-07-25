# com.hortifruti.sl.hortifruti.service.finance.transaction

CRUD e consultas de transações bancárias, relatório consolidado (BB + Sicoob) em PDF/Excel,
exportação em ZIP e persistência compartilhada de transações importadas via API.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `TransactionExportService.java` | `@Service` | Monta um ZIP com o relatório consolidado (Excel + PDF) do mês anterior mais os extratos bancários originais (PDF salvo + Excel gerado sob demanda para extratos de origem API) de cada `Statement` referenciado pelas transações do período. |
| `TransactionImportPersistenceService.java` | `@Service` | Persistência compartilhada pelos importadores de extrato (Sicoob, BB): filtra transações já existentes (por hash), salva em lote e, em caso de colisão de chave única, refaz a gravação uma a uma para isolar exatamente qual transação falhou. |
| `TransactionProcessingService.java` | `@Service` | CRUD e consultas de transações: listagem paginada com filtro por busca/tipo/categoria (via `Specification`), atualização, exclusão, listagem de categorias e cálculo de receita/despesa/saldo total por período (default: mês corrente). |
| `TransactionReportExcelGenerator.java` | `@Component` | Gera o relatório consolidado de transações (BB + Sicoob, ordem cronológica) em Excel, com estilo diferenciado para valores negativos (vermelho) e coluna de banco (BB/Sicoob/-). |
| `TransactionReportPdfGenerator.java` | `@Component` | Gera o relatório consolidado de transações em PDF (mesmo padrão de `AbstractPdfPageWriter` dos geradores de extrato bancário), com seção de resumo (total de entradas/saídas/saldo do período). |
| `TransactionReportService.java` | `@Service` | Fachada do relatório consolidado: resolve período (default mês anterior), busca transações ordenadas e delega para os geradores de PDF/Excel; monta nome de arquivo padrão. |
