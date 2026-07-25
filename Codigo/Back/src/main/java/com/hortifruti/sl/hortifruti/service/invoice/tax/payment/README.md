# com.hortifruti.sl.hortifruti.service.invoice.tax.payment

Gera o relatório "Resumo de Vendas por Forma de Pagamento" em PDF, para o período fiscal mensal.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `PaymentCalculator.java` | `@Service` | Soma o valor total das NF-e emitidas no período (via `InvoiceQuery`) sob a categoria fixa "Liquidação Bancária"; descarta valores inválidos/zero e erros por nota são apenas logados. |
| `PaymentPdfGenerator.java` | `@Component` | Desenha o PDF (cabeçalho com filial/CNPJ/período/situação, tabela por forma de pagamento com totais, seção "TIPO DE PAGAMENTO" fixa como DINHEIRO), com paginação automática. |
| `PaymentReport.java` | `@Component` | Fachada que encadeia `PaymentCalculator` + `PaymentPdfGenerator` para produzir o PDF final. |
