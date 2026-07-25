# com.hortifruti.sl.hortifruti.exception.finance

Exceção relacionada ao processamento de transações financeiras e conciliação bancária.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `TransactionException.java` | Exceção (`extends RuntimeException`) | Lançada em erros de processamento de transações (ex.: importação de extrato, categorização, duplicidade). Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
