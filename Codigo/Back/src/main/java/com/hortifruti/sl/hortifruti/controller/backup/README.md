# com.hortifruti.sl.hortifruti.controller.backup

Endpoints para disparo manual de backup do banco de dados para o Google Drive e para o fluxo de autorização OAuth2 associado.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `BackupController.java` | `@RestController` (`/backup`) | `POST /backup` executa o backup para um período opcional (`startDate`/`endDate`), retornando link de autorização caso necessário; `GET /backup/storage` retorna o tamanho atual do banco e o limite máximo em MB; `GET /backup/oauth2callback` recebe o `code` de retorno do fluxo OAuth2 do Google e delega a `GoogleOAuthService`. |
