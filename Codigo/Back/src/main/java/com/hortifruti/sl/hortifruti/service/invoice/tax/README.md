# com.hortifruti.sl.hortifruti.service.invoice.tax

Orquestra a geração mensal dos relatórios fiscais legais (ICMS, pagamento, registro de saídas,
vendas, XMLs de notas de saída) e fornece o suporte comum de desenho de PDF usado por todos os
relatórios do pacote.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `PdfReportSupport.java` | classe utilitária (`final`) | Layout compartilhado dos relatórios em PDF (margens, largura de tabela, altura de célula) e helpers de desenho (texto, cabeçalho/linha de tabela com colunas de largura igual, formatação de valor com vírgula). |
| `ReportTaxService.java` | `@Service` | Gera o pacote mensal de relatórios fiscais: monta ZIP único (`generateMonthly`) ou mapa de arquivos soltos (`generateMonthlyFiles`, usado no relatório macro), agregando pagamento, registro de saída, vendas, ICMS e XMLs de NF-e; cada relatório falha de forma isolada (best-effort) sem interromper os demais. |

## Subpacotes

- `icms/` — relatório de apuração de ICMS por CFOP (ver icms/README.md)
- `nfSales/` — coleta e empacotamento em ZIP dos XMLs de NF-e de saída do período (ver nfSales/README.md)
- `payment/` — resumo de vendas por forma de pagamento (ver payment/README.md)
- `registerReport/` — livro de registro de saídas (modelo P 2/A) (ver registerReport/README.md)
- `sales/` — relação de vendas do período (ver sales/README.md)
