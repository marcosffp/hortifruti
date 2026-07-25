# com.hortifruti.sl.hortifruti.exception.storage

Exceção relacionada ao armazenamento de arquivos no Cloudflare R2.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `StorageException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas de upload, download ou remoção de arquivos no Cloudflare R2 (boletos, XMLs de nota fiscal, extratos). Tratada pelo `GlobalExceptionHandler` com status **500 Internal Server Error**, com log de erro. |
