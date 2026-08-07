# com.hortifruti.sl.hortifruti.service.googleauth

Gerencia o ciclo de vida das credenciais OAuth2 do Google (Drive + Gmail): obtenção,
validação/renovação de token e recuperação de erros de autorização (`invalid_grant`). Usa a Google
API Client Library (`GoogleAuthorizationCodeFlow`, `FileDataStoreFactory`). Pacote neutro — não
pertence a `backup` nem a `notification`, já que ambos reaproveitam o mesmo fluxo/token store
(`CredentialConfig.authOrigin` identifica qual dos dois iniciou a autorização, para o callback
saber para onde redirecionar o usuário).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `AuthorizationHandler.java` | `@Component` | Executa o fluxo interativo de autorização (`AuthorizationCodeInstalledApp` + `LocalServerReceiver` na porta 8888) para obter um novo `Credential` quando necessário. |
| `CredentialConfig.java` | classe `@Builder` | DTO imutável com nome da aplicação, diretório de tokens, redirect URI e arquivo de credenciais, usado para parametrizar a obtenção de credenciais. |
| `CredentialManager.java` | `@Component` | Ponto central: monta o `GoogleAuthorizationCodeFlow`, tenta carregar credencial existente, valida (via `TokenValidator`) e, se inválida, gera URL de autorização (`AUTHORIZATION_REQUIRED:` embutido no access token) ou delega erros de token a `TokenExceptionHandler`. Usado por `service.backup.auth.GoogleAuthService` (Drive) e `service.notification.email.GmailApiEmailSender` (Gmail API). |
| `TokenCleaner.java` | `@UtilityClass` | Apaga recursivamente o diretório de tokens salvos em disco, usado ao recuperar de um `invalid_grant`. |
| `TokenExceptionHandler.java` | `@Component` | Trata `TokenResponseException`: se for `invalid_grant`, limpa tokens e reinicia o fluxo de autorização; caso contrário, propaga como `BackupException`. |
| `TokenValidator.java` | `@Component` | Verifica se um `Credential` ainda é válido (token não expirado ou refresh bem-sucedido). |
