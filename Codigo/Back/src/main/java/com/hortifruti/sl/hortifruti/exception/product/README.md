# com.hortifruti.sl.hortifruti.exception.product

Exceções do módulo de produtos fiscais.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `InvalidConversaoCaixaFileException.java` | Exceção (`extends DomainException`) | Lançada pelo `ConversaoCaixaImportService` quando o CSV de conversão caixa→kg está ausente, ilegível ou sem as colunas `COD`/`UNIDADE`/`KG` esperadas. Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
