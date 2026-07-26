# com.hortifruti.sl.hortifruti.dto.purchase.client

DTOs de cadastro, atualização e consulta de clientes, incluindo variações resumidas usadas em
listagens e seletores (dropdowns) na tela de compras.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ClientLastGroupingResponse.java` | record | Última compra agrupada (combined score) de um cliente: id do cliente, data de confirmação e valor total. |
| `ClientRequest.java` | record | Payload de request para cadastro/edição de cliente: nome (`@NotBlank`), preço variável, e-mail (`@Email`), telefone e documento (`@NotBlank`), endereço, inscrição estadual, indicador estadual, código CIDE e flag `onlyBillet` (cliente que só recebe boleto, sem nota fiscal). |
| `ClientResponse.java` | record | Resposta com dados completos do cliente: id, nome, e-mail, telefone, endereço, documento, preço variável, inscrição/indicador estadual, código CIDE, flag `onlyBillet`, data da última compra e valor total de compras. |
| `ClientSelectionInfo.java` | record | Par mínimo id/nome do cliente, usado em componentes de seleção (dropdowns). |
| `ClientSummary.java` | record | Resumo de cliente para relatórios: nome, endereço, total de produtos e valor total. |
| `ClientWithLastPurchaseResponse.java` | record | Cliente com dados da última compra: id, nome, data e valor total da última compra. |
