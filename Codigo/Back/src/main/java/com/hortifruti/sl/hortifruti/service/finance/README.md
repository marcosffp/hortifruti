# com.hortifruti.sl.hortifruti.service.finance

Módulo financeiro: ponto de entrada único para extratos bancários (BB e Sicoob), exportação
consolidada de relatórios bancários e fiscais em ZIP ("relatório macro"), e base comum de desenho
de PDF usada pelos geradores de extrato/relatório do módulo.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `AbstractPdfPageWriter.java` | classe abstrata | Base comum para os "PageWriter" que desenham extratos/relatórios em PDF via PDFBox (paginação automática com `ensureSpace`, texto alinhado à esquerda/direita, truncamento); usada por `SicoobExtratoPdfGenerator`, `BBExtratoPdfGenerator` e `TransactionReportPdfGenerator`. |
| `MacroExportService.java` | `@Service` | Gera o "Relatório Macro" mensal em ZIP, combinando relatórios bancários (`TransactionExportService`) e relatórios fiscais (`ReportTaxService`) em pastas separadas; se a geração fiscal falhar, grava um arquivo de aviso e continua sem interromper a exportação. |
| `SicoobExtratoFormatUtil.java` | classe utilitária (final, construtor privado) | Formata valores monetários e datas no padrão do extrato do Sicoob (ex.: `"R$ 1.234,56 C"`), compartilhado entre geração de PDF e Excel do extrato (BB e Sicoob) e do relatório consolidado; movida de `util/` por só ser usada neste módulo. |
| `StatementService.java` | `@Service` | Fachada para consulta/geração de extratos: listagem geral, download do arquivo já salvo (R2 ou coluna legada) e delegação das operações específicas de cada banco para `BBStatementService`/`SicoobStatementService`. |

## Subpacotes

- `bb/` — integração com a API Extratos v2 do Banco do Brasil (saldo, extrato, import) (ver bb/README.md)
- `sicoob/` — integração com a API de conta corrente do Sicoob (extrato, import) (ver sicoob/README.md)
- `transaction/` — CRUD, relatórios consolidados e persistência de transações importadas (ver transaction/README.md)
