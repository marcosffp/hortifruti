# com.hortifruti.sl.hortifruti.exception.purchase

Exceções do módulo de compras/vendas, clientes e agrupamento de pontuação para cobrança combinada.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `PurchaseException.java` | Exceção (`extends RuntimeException`) | Lançada em erros de processamento de compras (ex.: dados inválidos, status inválido — ver `Status.fromString` em `model.purchase`). Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `ClientException.java` | Exceção (`extends RuntimeException`) | Lançada em erros de cadastro/consulta de clientes. Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `CombinedScoreException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas no agrupamento de pontuação combinada de compras (`CombinedScore`) para geração conjunta de boleto/nota fiscal. Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
