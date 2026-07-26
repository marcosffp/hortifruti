# com.hortifruti.sl.hortifruti.config.storage

Configuração do cliente de object storage (Cloudflare R2, compatível com S3) usado para armazenar boletos, notas fiscais e extratos.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `R2Config.java` | `@Configuration` | Declara o bean `S3Client` (AWS SDK v2) apontado para o endpoint do Cloudflare R2, com credenciais estáticas (`r2.access-key-id`/`r2.secret-access-key`), região `auto` e acesso path-style habilitado (exigido pelo R2). |
