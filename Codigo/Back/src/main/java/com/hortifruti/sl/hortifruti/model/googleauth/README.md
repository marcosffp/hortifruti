# com.hortifruti.sl.hortifruti.model.googleauth

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `GoogleOAuthToken.java` | `@Entity` (`google_oauth_tokens`) | Credencial OAuth2 do Google (Drive/Gmail) serializada e criptografada (AES-GCM), no lugar do arquivo local que o `FileDataStoreFactory` do google-oauth-client usaria por padrão. `storeKey` combina o `dataStoreId` da biblioteca com a chave do usuário — ver `service.googleauth.DatabaseDataStore`. |
