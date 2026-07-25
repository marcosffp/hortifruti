# com.hortifruti.sl.hortifruti.dto.finance

DTOs do módulo financeiro/conciliação bancária: saldo de conta, extratos importados e transações
lançadas, usados nos fluxos de consulta de saldo (BB) e de cadastro/listagem de lançamentos
financeiros.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BankBalanceResponse.java` | record | Resposta mínima de saldo bancário: `saldoDisponivel`, flag `detalhado` e `consultadoEm`. Deliberadamente não inclui agência, conta ou lançamentos, para não expor mais dados da API do BB do que o necessário ao front. |
| `StatementResponse.java` | record | Representa um extrato importado: id, nome, banco (`Bank`), origem (`StatementOrigin`) e data de criação. |
| `TransactionRequest.java` | record | Payload de request para cadastro de uma transação financeira, com validação `@NotNull`/`@NotBlank` em data, histórico, valor, categoria, tipo de transação, documento, agência de origem e lote. |
| `TransactionRequestDate.java` | record | Filtro simples de período (`startDate`/`endDate`) usado em consultas de transações. |
| `TransactionResponse.java` | record | Resposta com os dados de uma transação: id, documento, histórico, categoria, tipo, data, valor, banco e origem do extrato. |
