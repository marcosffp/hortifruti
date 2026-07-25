# com.hortifruti.sl.hortifruti.exception.invoice

Exceções do módulo de nota fiscal, relacionado à integração com a Focus NFe.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `InvoiceException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas gerais de emissão, consulta ou cancelamento de nota fiscal via Focus NFe. Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `InvoiceAlreadyCancelledException.java` | Exceção (`extends InvoiceException`) | Lançada quando a Focus NFe recusa o cancelamento porque a NF-e já estava cancelada (código `already_processed`). Deve ser tratada pelo chamador como sucesso/no-op, não como falha real, já que representa o estado desejado da operação. |
