# com.hortifruti.sl.hortifruti.model.purchase

Entidades centrais do domínio de compras/vendas: clientes, compras individuais (originadas de notas fiscais) e o agrupamento delas em cobranças combinadas (boleto + nota fiscal).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `Client.java` | `@Entity` (`clients`) | Cliente da hortifruti. Guarda dados cadastrais (`clientName`, `email` único, `phoneNumber`, `address`, `document`), flags de regra de negócio (`variablePrice`, `onlyBillet`) e dados fiscais (`stateRegistration`, `stateIndicator`, `cideCode`). Mantém `totalPurchaseValue`/`lastPurchaseDate` como cache. Relaciona `@OneToMany` com `Purchase` (`purchases`, cascade `ALL`, `@JsonIgnore`). |
| `Purchase.java` | `@Entity` (`purchases`) | Compra individual de um cliente, referenciando `Client` via `@ManyToOne` (`client_id`, obrigatório). Guarda `purchaseDate` e `total`. Relaciona `@OneToMany` com `InvoiceProduct` (`invoiceProducts`, cascade `ALL` + `orphanRemoval`, `@JsonIgnore`). |
| `InvoiceProduct.java` | `@Entity` (`invoice_products`) | Item de produto de uma compra, referenciando `Purchase` via `@ManyToOne` (`purchase_id`, obrigatório, `@JsonIgnore`). Guarda `code`, `name`, `price`, `unitType`, `quantity`. |
| `CombinedScore.java` | `@Entity` (`combined_scores`) | Agrupamento de compras de um cliente (`clientId`, FK crua) em uma cobrança combinada, com `status` (`Status`, default `PENDENTE`), flags `hasBillet`/`hasInvoice`, `dueDate`, `totalValue`, e referências ao boleto (`ourNumber_sicoob`, `yourNumber`) e à nota fiscal (`invoiceRef`) gerados. Relaciona `@OneToMany` com `GroupedProduct` (`groupedProducts`, cascade `ALL` + `orphanRemoval`). |
| `GroupedProduct.java` | `@Entity` (`grouped_product`) | Produto agregado (somado entre compras) dentro de um `CombinedScore`, referenciado via `@ManyToOne` (`combined_score_id`, obrigatório). Guarda `code`, `name`, `price`, `quantity`, `totalValue`. |
| `Status.java` | Enum | Status de uma compra/cobrança combinada: `PENDENTE`, `PAGO`, `CANCELADO`, `CANCELADO_BOLETO`, `CANCELADO_NOTA_FISCAL`. Método `fromString` lança `PurchaseException` se o valor for inválido/vazio. |
