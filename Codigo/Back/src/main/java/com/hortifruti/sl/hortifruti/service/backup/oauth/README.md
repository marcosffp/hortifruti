# com.hortifruti.sl.hortifruti.service.backup.oauth

Implementa o fluxo OAuth2 "authorization code" do Google usado para a primeira autorização (ou reautorização) de acesso ao Drive/Gmail: recebe o código de autorização do callback HTTP, troca por tokens e os persiste no banco (não mais em disco) via `service.googleauth.DatabaseDataStoreFactory`. Complementar a `backup/auth`, que cuida da validação/renovação de credenciais já obtidas.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `AuthorizationFlowFactory.java` | `@Component` | Monta o `GoogleAuthorizationCodeFlow` (transporte HTTP, client secrets decodificados via `Base64FileDecoder`, persistência via `DatabaseDataStoreFactory`) e o empacota em `OAuthFlowContext`. |
| `GoogleOAuthService.java` | `@Service` | Endpoint de callback: processa o código de autorização e redireciona o navegador para o frontend com `?auth=success` ou `?auth=error&message=...`. |
| `OAuthCallbackHandler.java` | `@Component` | Orquestra a troca do código de autorização por token, delegando a criação do fluxo a `AuthorizationFlowFactory` e a troca/persistência a `TokenProcessor`. |
| `OAuthFlowContext.java` | classe `@Builder` | DTO imutável que agrupa `NetHttpTransport` e `GoogleAuthorizationCodeFlow` para passar entre as etapas do fluxo OAuth. |
| `TokenProcessor.java` | `@Component` | Troca o código de autorização por `TokenResponse` (usando o `redirectUri` configurado) e persiste a credencial resultante via `flow.createAndStoreCredential`. |
