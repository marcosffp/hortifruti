# com.hortifruti.sl.hortifruti.model.finance

Entidades e enums da conciliação bancária: extratos importados/consultados via API e as transações neles contidas.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `Statement.java` | `@Entity` (`statements`) | Extrato bancário. Guarda `bank` (`Bank`) e `origin` (`StatementOrigin`, default `PDF_UPLOAD`). `filePath` (`byte[]`, `@Lob`) é legado; extratos novos usam `objectKey` (Cloudflare R2). Para `origin = API`, guarda `periodStart`/`periodEnd` para evitar reconsultar o banco. Relaciona `@OneToMany` com `Transaction` (`transactions`, cascade `ALL` + `orphanRemoval`). |
| `Transaction.java` | `@Entity` (`transactions`) | Transação financeira dentro de um extrato, referenciando `Statement` via `@ManyToOne` (`statement_id`, obrigatório). Guarda `amount` (`BigDecimal`), `category` (`Category`), `transactionType` (`TransactionType`) e `hash` (único, usado para deduplicação). `equals`/`hashCode` sobrescritos com base em `transactionDate`, `document`, `amount` e `history`. |
| `Bank.java` | Enum | Bancos suportados: `BANCO_DO_BRASIL`, `SICOOB`, `UNKNOWN`. |
| `StatementOrigin.java` | Enum | Origem do extrato: `PDF_UPLOAD` (upload manual, fluxo legado) ou `API` (consulta às APIs do Sicoob/BB, forma atual). |
| `TransactionType.java` | Enum | Tipo de lançamento: `DEBITO`, `CREDITO`. |
| `Category.java` | Enum | Categorias de classificação de transações: `VENDAS_CARTAO`, `VENDAS_PIX`, `SERVICOS_BANCARIOS`, `FORNECEDOR`, `FAMILIA`, `FUNCIONARIO`, `SERVICOS_TELEFONICOS`, `CEMIG`, `COPASA`, `FISCAL`, `IMPOSTOS`. |
