# com.hortifruti.sl.hortifruti.service.backup.auth

Monta o cliente `Drive` autenticado usado pelo backup. O gerenciamento de credenciais OAuth2 do
Google em si (obtenção, validação/renovação de token, recuperação de `invalid_grant`) é
compartilhado com `notification.email` (Gmail API) e vive em `service.googleauth`.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `GoogleAuthService.java` | `@Service` | Monta o cliente `Drive` autenticado a partir de `CredentialConfig`/`CredentialManager` (`service.googleauth`); propaga como `BackupException("AUTHORIZATION_REQUIRED:...")` quando o usuário precisa reautorizar. Mantém o `Drive`/`Credential` em cache enquanto o access token não estiver perto de expirar, evitando recriar o cliente a cada chamada dentro da mesma operação de backup. |
