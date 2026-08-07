# com.hortifruti.sl.hortifruti.service.googleauth

Gerencia o ciclo de vida das credenciais OAuth2 do Google (Drive + Gmail): obtenção,
validação/renovação de token e recuperação de erros de autorização (`invalid_grant`). Usa a Google
API Client Library (`GoogleAuthorizationCodeFlow`) com persistência própria em banco (tabela
`google_oauth_tokens`, criptografada em repouso) em vez de `FileDataStoreFactory` — necessário
porque o filesystem do container é efêmero (reinícios/redeploys perderiam os tokens, forçando
reautenticação manual). Pacote neutro — não pertence a `backup` nem a `notification`, já que ambos
reaproveitam o mesmo fluxo/token store (`CredentialConfig.authOrigin` identifica qual dos dois
iniciou a autorização, para o callback saber para onde redirecionar o usuário).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `CredentialConfig.java` | classe `@Builder` | DTO imutável com nome da aplicação, redirect URI e arquivo de credenciais, usado para parametrizar a obtenção de credenciais. |
| `CredentialManager.java` | `@Component` | Ponto central: monta o `GoogleAuthorizationCodeFlow`, tenta carregar credencial existente, valida (via `TokenValidator`) e, se inválida, gera URL de autorização (`AUTHORIZATION_REQUIRED:` embutido no access token) ou delega erros de token a `TokenExceptionHandler`. Usado por `service.backup.auth.GoogleAuthService` (Drive) e `service.notification.email.GmailApiEmailSender` (Gmail API). |
| `DatabaseDataStoreFactory.java` / `DatabaseDataStore.java` | `@Component` / package-private | Implementação de `DataStoreFactory`/`DataStore` do google-oauth-client que persiste no banco via `GoogleOAuthTokenRepository`, no lugar de `FileDataStoreFactory`. |
| `TokenEncryptionService.java` | `@Component` | Criptografa/descriptografa (AES-GCM) o conteúdo salvo em `google_oauth_tokens`; chave vem de `google.tokens.encryption-key`. |
| `TokenExceptionHandler.java` | `@Component` | Trata `TokenResponseException`: se for `invalid_grant`, limpa a credencial persistida e devolve o sentinel `AUTHORIZATION_REQUIRED:<url>` (mesmo padrão usado quando nunca houve credencial) para reautorização via navegador; caso contrário, propaga como `BackupException`. |
| `TokenValidator.java` | `@Component` | Verifica se um `Credential` ainda é válido (token não expirado ou refresh bem-sucedido). |
