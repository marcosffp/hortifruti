# com.hortifruti.sl.hortifruti.exception.sicoob

Exceção relacionada à consulta de extrato bancário via API do Sicoob.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `SicoobExtratoException.java` | Exceção (`extends RuntimeException`) | Lançada quando falha a consulta de extrato bancário na API do Sicoob. Tratada pelo `GlobalExceptionHandler` com status **502 Bad Gateway**, com log de erro. |
