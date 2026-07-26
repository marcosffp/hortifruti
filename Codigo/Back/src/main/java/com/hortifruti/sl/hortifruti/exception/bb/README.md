# com.hortifruti.sl.hortifruti.exception.bb

Exceção relacionada à integração com a API do Banco do Brasil.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BBApiException.java` | Exceção (`extends RuntimeException`) | Lançada quando ocorre falha na comunicação com a API do Banco do Brasil (ex.: erro ao consultar extrato ou autenticar). Tratada pelo `GlobalExceptionHandler` com status **502 Bad Gateway**. |
