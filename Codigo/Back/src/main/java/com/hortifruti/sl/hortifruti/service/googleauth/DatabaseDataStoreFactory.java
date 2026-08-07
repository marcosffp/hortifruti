package com.hortifruti.sl.hortifruti.service.googleauth;

import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.DataStore;
import com.hortifruti.sl.hortifruti.repository.googleauth.GoogleOAuthTokenRepository;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Fábrica de {@link DataStore} usada no lugar de {@code FileDataStoreFactory} nos fluxos OAuth2 do
 * Google (backup + notificações) — ver {@link DatabaseDataStore}.
 */
@Component
@RequiredArgsConstructor
public class DatabaseDataStoreFactory extends AbstractDataStoreFactory {

  private final GoogleOAuthTokenRepository repository;
  private final TokenEncryptionService encryptionService;

  @Override
  protected <V extends Serializable> DataStore<V> createDataStore(String id) {
    return new DatabaseDataStore<>(this, id, repository, encryptionService);
  }
}
