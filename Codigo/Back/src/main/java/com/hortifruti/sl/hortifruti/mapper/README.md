# com.hortifruti.sl.hortifruti.mapper

Interfaces MapStruct (`componentModel = "spring"`) que convertem entre entidades JPA (`model`) e DTOs (`dto`), evitando conversão manual repetitiva nos serviços/controllers. A implementação de cada interface é gerada em tempo de compilação pelo processador de anotações do MapStruct.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `ClientMapper.java` | `interface` (`@Mapper`) | Converte `ClientRequest` ↔ `Client` ↔ `ClientResponse`, incluindo campos fiscais (inscrição estadual, CIDE) e a flag `onlyBillet` ("somente boleto"). |
| `CombinedScoreMapper.java` | `interface` (`@Mapper`) | Converte `CombinedScoreRequest` ↔ `CombinedScore` ↔ `CombinedScoreResponse` (cobrança combinada boleto+nota fiscal), incluindo atualização parcial via `updateEntityFromRequest` (`@MappingTarget`); ignora `id`, `confirmedAt` e `totalValue` na escrita. |
| `FreightConfigMapper.java` | `interface` (`@Mapper`) | Converte `FreightConfig` → `FreightConfigDTO` e aplica atualização parcial de DTO sobre a entidade (`updateEntityFromDTO`, ignorando `id`). |
| `GroupedProductMapper.java` | `interface` (`@Mapper`) | Converte `GroupedProduct` (produto agrupado em uma venda/nota) → `GroupedProductResponse`. |
| `InvoiceProductMapper.java` | `interface` (`@Mapper`) | Converte `InvoiceProduct` → `InvoiceProductResponse`, incluindo tipo de unidade (`unitType`). |
| `ProductMapper.java` | `interface` (`@Mapper`) | Converte `ProductRequest` ↔ `ClimateProduct` ↔ `ProductResponse` (produto sazonal/clima); `toProductResponse` é implementado manualmente (`default`) em vez de gerado. |
| `PurchaseMapper.java` | `interface` (`@Mapper`) | Converte `Purchase` → `PurchaseResponse` (data da compra, total, última atualização). |
| `TransactionMapper.java` | `interface` (`@Mapper`) | Converte `Transaction` ↔ `TransactionResponse`/`TransactionRequest`, incluindo dados do extrato associado (`bank`, `origin`); expõe também uma fábrica `toTransaction` com parâmetros posicionais e atualização parcial ignorando campos de auditoria (`id`, `createdAt`, `updatedAt`, `hash`). |
| `UserMapper.java` | `interface` (`@Mapper`) | Converte `UserRequest` → `User` e `User` → `UserResponse` (username, cargo, papel). |
