# com.hortifruti.sl.hortifruti.exception.storage

Exceções relacionadas ao armazenamento de arquivos no Cloudflare R2.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `StorageException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas de upload, download ou remoção de arquivos no Cloudflare R2 (boletos, XMLs de nota fiscal, extratos). Tratada pelo `GlobalExceptionHandler` com status **500 Internal Server Error**, com log de erro. |
| `StorageNotFoundException.java` | Exceção (`extends StorageException`) | Lançada quando o arquivo simplesmente não existe (ex.: boleto nunca foi gerado, ou gerado antes desta funcionalidade existir) — condição esperada do domínio, não uma falha de infraestrutura. Herda o tipo de `StorageException`, mas o `GlobalExceptionHandler` tem handler próprio (mais específico) que responde **404 Not Found** com log de warn em vez de erro. |
