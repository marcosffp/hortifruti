# com.hortifruti.sl.hortifruti.service.storage

Camada de armazenamento de arquivos no Cloudflare R2 (compatível com S3): upload/download/movimentação genéricos e um serviço especializado para o ciclo de vida dos PDFs de boletos gerados.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `R2StorageService.java` | `@Service` | Operações genéricas no bucket R2 via `S3Client`: `upload` (lança `StorageException` em falha, pois a operação de negócio não deve ser considerada concluída), `download`, `delete` (desfaz upload órfão) e `moveToCancelled` (copy + delete, só apaga o original se a cópia for bem-sucedida, para nunca perder o arquivo). |
| `StorageKeyGenerator.java` | classe utilitária (métodos estáticos) | Gera chaves de objeto no padrão `prefixo/ambiente/AAAA/MM/idNegocio_timestamp.ext` (ex.: `boletos/prod/2026/07/42_20260718_143210.pdf`) e uma variante que insere um segmento extra (ex.: `cancelados`) logo após prefixo/ambiente, preservando o restante da chave original. |
| `BilletFileStorageService.java` | `@Service` | Ciclo de vida do PDF de um boleto: salva de forma idempotente (reaproveita arquivo ativo existente em reprocessamento/retry), baixa o conteúdo do boleto ativo, e agenda a movimentação para a pasta de cancelados **após o commit da transação** (via `TransactionSynchronizationManager`) para garantir que o cancelamento já esteja confirmado no banco antes de tocar no R2. |
