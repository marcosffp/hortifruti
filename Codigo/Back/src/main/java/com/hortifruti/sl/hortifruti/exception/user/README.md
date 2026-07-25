# com.hortifruti.sl.hortifruti.exception.user

Exceção relacionada ao cadastro e gestão de usuários do sistema.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `UserException.java` | Exceção (`extends RuntimeException`) | Lançada em erros de cadastro/consulta de usuários (ex.: papel/role inválido, ver `Role.fromString` em `model`). Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
