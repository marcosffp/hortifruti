# com.hortifruti.sl.hortifruti.repository.googleauth

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `GoogleOAuthTokenRepository.java` | `JpaRepository<GoogleOAuthToken, String>` | `findAllByStoreKeyStartingWith`/`deleteAllByStoreKeyStartingWith` dão a `service.googleauth.DatabaseDataStore` as operações por `dataStoreId` (prefixo da chave) que o `DataStore` do google-oauth-client precisa (`keySet`, `values`, `clear`). |
