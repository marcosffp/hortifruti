# com.hortifruti.sl.hortifruti.exception.auth

Exceções relacionadas a autenticação, autorização e proteção contra tentativas de login abusivas.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `AuthException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas genéricas de autenticação (ex.: usuário inexistente, senha incorreta). Tratada pelo `GlobalExceptionHandler` com status **401 Unauthorized**. |
| `AccountLockedException.java` | Exceção (`extends AuthException`) | Lançada quando a conta ou o IP está bloqueado por excesso de tentativas de login (mecanismo de lockout). Carrega `retryAfterSeconds` para informar ao front quando reabilitar o botão de login. Herda o tratamento de `AuthException` (401), mas o handler global inclui `retryAfter` no corpo da resposta. |
| `TokenException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas relacionadas a tokens (JWT/refresh token) inválidos ou expirados. Tratada pelo `GlobalExceptionHandler` com status **403 Forbidden**. |
