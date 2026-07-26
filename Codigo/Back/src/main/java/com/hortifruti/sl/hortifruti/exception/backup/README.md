# com.hortifruti.sl.hortifruti.exception.backup

Exceção relacionada ao processo de backup automatizado para o Google Drive.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BackupException.java` | Exceção (`extends RuntimeException`) | Lançada quando falha a geração ou o envio do backup (ex.: erro de comunicação com o Google Drive). Tratada pelo `GlobalExceptionHandler` com status **500 Internal Server Error**, registrando o stack trace completo no log. |
