# com.hortifruti.sl.hortifruti.service.backup

Orquestra o backup periódico dos dados transacionais (compras, produtos de nota, transações e extratos) para o Google Drive: gera CSVs, envia para uma estrutura de pastas por ano/mês/entidade e remove do banco os registros já salvos. Depende de `backup/folders` (upload/pastas no Drive) e `scheduler.DatabaseStorageMonitorService` (tamanho do banco).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BackupPathService.java` | `@Service` | Calcula/cria a hierarquia de pastas no Drive `backups/{ano}/{mês}/{entidade}_{período}`, reaproveitando pastas já existentes. |
| `BackupService.java` | `@Service` | Fachada do backup: gera os CSVs do período, faz upload de cada um ao Drive, apaga os arquivos temporários locais, aciona a limpeza das entidades no banco e trata o fluxo de link de autorização OAuth quando necessário (`AUTHORIZATION_REQUIRED:`). Também expõe o tamanho atual/máximo do banco. |
| `CsvGeneratorService.java` | `@Service` | Gera os 4 arquivos CSV (compras, produtos de nota, transações, extratos) de um período em `java.io.tmpdir`, com cabeçalhos em português. |
| `EntityCleanupService.java` | `@Service` | Remove do banco, dentro de uma transação, os registros de compras/produtos/transações/extratos já cobertos pelo CSV gerado para o período. |

## Subpacotes

- `auth/` — monta o cliente `Drive` autenticado (ver `auth/README.md`); o gerenciamento de credenciais OAuth2 do Google em si é compartilhado com `notification.email` e vive em `service.googleauth`.
- `folders/` — operações de baixo nível no Google Drive: busca/criação de pastas e upload de arquivos (ver `folders/README.md`).
- `oauth/` — fluxo de callback OAuth2 (troca de código de autorização por token) usado no primeiro login/autorização do Drive (ver `oauth/README.md`).
