# com.hortifruti.sl.hortifruti.service.backup.auth

Gerencia o ciclo de vida das credenciais OAuth2 do Google (Drive + Gmail) usadas pelo backup e pelo envio de notificações por e-mail: obtenção, validação/renovação de token e recuperação de erros de autorização (`invalid_grant`). Usa a Google API Client Library (`GoogleAuthorizationCodeFlow`).

A credencial é persistida no MySQL via `DatabaseDataStoreFactory`, não em arquivo local: o disco do container em produção é efêmero, então qualquer redeploy/crash/ciclo de sleep apagaria um arquivo e forçaria o usuário a reautorizar o Google — o banco sobrevive a esses reinícios.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `CredentialConfig.java` | classe `@Builder` | DTO imutável com nome da aplicação, redirect URI e arquivo de credenciais, usado para parametrizar a obtenção de credenciais. |
| `CredentialManager.java` | `@Component` | Ponto central: monta o `GoogleAuthorizationCodeFlow`, tenta carregar credencial existente, valida (via `TokenValidator`) e, se inválida, gera URL de autorização (`AUTHORIZATION_REQUIRED:` embutido no access token) ou delega erros de token a `TokenExceptionHandler`. |
| `DatabaseDataStoreFactory.java` | `@Component` | Implementação de `DataStoreFactory`/`DataStore` da Google API Client Library que persiste a credencial OAuth em `oauth_credential_entries` (MySQL) em vez de em arquivo, via `OAuthCredentialEntryRepository`. |
| `GoogleAuthService.java` | `@Service` | Monta o cliente `Drive` autenticado a partir de `CredentialConfig`; propaga como `BackupException("AUTHORIZATION_REQUIRED:...")` quando o usuário precisa reautorizar. |
| `TokenExceptionHandler.java` | `@Component` | Trata `TokenResponseException`: se for `invalid_grant`, apaga a credencial persistida e devolve um novo link de autorização (mesmo formato de `CredentialManager`); caso contrário, propaga como `BackupException`. |
| `TokenValidator.java` | `@Component` | Verifica se um `Credential` ainda é válido (token não expirado ou refresh bem-sucedido). |
