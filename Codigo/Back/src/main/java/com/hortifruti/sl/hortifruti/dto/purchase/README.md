# com.hortifruti.sl.hortifruti.dto.purchase

DTOs do módulo de compras/vendas: agrupamento de notas fiscais em "combined score" (fechamento de
compra de um cliente), produtos de nota fiscal e agrupados, boletos avulsos e edição de itens de
compra. Subpacote `client` concentra os DTOs de cadastro/consulta de clientes.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `CombinedScoreRequest.java` | record | Payload de request para criar um combined score (fechamento de período de compra de um cliente): id do cliente, data de início/fim (`@NotNull`) e data de confirmação opcional. |
| `CombinedScoreResponse.java` | record | Resposta de um combined score: id, id do cliente, valor total, vencimento, data de confirmação, status (`Status`), flags se já tem boleto/nota fiscal, número do documento (`yourNumber`) e referência da nota (`invoiceRef`). |
| `CombinedScoreSummaryResponse.java` | record | Resumo de combined scores de um cliente: nome do cliente, total de itens, valor total e lista de `CombinedScoreDetails` (record aninhado com data de confirmação, vencimento, valor, status e flags de boleto/nota). |
| `GroupedProductResponse.java` | record | Produto agrupado (somado entre notas) de uma compra: código, nome, preço, quantidade e valor total. |
| `InvoiceProductResponse.java` | record | Produto de uma nota fiscal específica: id, código, nome, preço, quantidade e tipo de unidade. |
| `NotaUploadResponse.java` | record | Resposta do upload de foto de nota de compra (`POST /api/compras/notas/upload`): id do arquivo temporário, tamanho em bytes e content-type real (detectado por magic bytes). |
| `NotaExtracaoResponse.java` | record | Resposta da extração via Gemini (`POST /api/compras/notas/extrair`): nome do cliente/destinatário, data da nota, lista de `ItemNotaExtraido` e total geral — todos nulláveis exceto a lista de itens, já que campos ilegíveis vêm como `null` em vez de valor inventado. |
| `ItemNotaExtraido.java` | record | Um item lido da nota pelo Gemini: nome do produto como está escrito (sem correção de ortografia), quantidade, unidade, preço unitário e total da linha — todos nulláveis exceto o nome do produto. |
| `PurchaseResponse.java` | record | Resposta de uma compra: id, data da compra, total e data de atualização. |
| `UpdateGroupedProduct.java` | record | Payload para atualizar um produto agrupado: nome, preço e quantidade. |
| `UpdateInvoiceProduct.java` | record | Payload para atualizar um produto de nota fiscal: código, nome, preço, quantidade e tipo de unidade. |
| `WildcardBilletRequest.java` | record | Request para gerar um boleto avulso (sem nota fiscal vinculada): id do cliente (`@NotNull`) e valor (`@NotNull`, `@Positive`). |

## Subpacotes

- `client/` — cadastro, consulta e resumo de clientes.
