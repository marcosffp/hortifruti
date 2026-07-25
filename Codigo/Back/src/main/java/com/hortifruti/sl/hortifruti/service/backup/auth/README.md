# com.hortifruti.sl.hortifruti.service.backup.auth

Gerencia o ciclo de vida das credenciais OAuth2 do Google (Drive + Gmail) usadas pelo backup: obtenção, validação/renovação de token e recuperação de erros de autorização (`invalid_grant`). Usa a Google API Client Library (`GoogleAuthorizationCodeFlow`, `FileDataStoreFactory`).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `AuthorizationHandler.java` | `@Component` | Executa o fluxo interativo de autorização (`AuthorizationCodeInstalledApp` + `LocalServerReceiver` na porta 8888) para obter um novo `Credential` quando necessário. |
| `CredentialConfig.java` | classe `@Builder` | DTO imutável com nome da aplicação, diretório de tokens, redirect URI e arquivo de credenciais, usado para parametrizar a obtenção de credenciais. |
| `CredentialManager.java` | `@Component` | Ponto central: monta o `GoogleAuthorizationCodeFlow`, tenta carregar credencial existente, valida (via `TokenValidator`) e, se inválida, gera URL de autorização (`AUTHORIZATION_REQUIRED:` embutido no access token) ou delega erros de token a `TokenExceptionHandler`. |
| `GoogleAuthService.java` | `@Service` | Monta o cliente `Drive` autenticado a partir de `CredentialConfig`; propaga como `BackupException("AUTHORIZATION_REQUIRED:...")` quando o usuário precisa reautorizar. |
| `TokenCleaner.java` | `@UtilityClass` | Apaga recursivamente o diretório de tokens salvos em disco, usado ao recuperar de um `invalid_grant`. |
| `TokenExceptionHandler.java` | `@Component` | Trata `TokenResponseException`: se for `invalid_grant`, limpa tokens e reinicia o fluxo de autorização; caso contrário, propaga como `BackupException`. |
| `TokenValidator.java` | `@Component` | Verifica se um `Credential` ainda é válido (token não expirado ou refresh bem-sucedido). |
