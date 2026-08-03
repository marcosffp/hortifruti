package com.hortifruti.sl.hortifruti.service.backup.auth;

import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.AbstractMemoryDataStore;
import com.google.api.client.util.store.DataStore;
import com.hortifruti.sl.hortifruti.model.OAuthCredentialEntry;
import com.hortifruti.sl.hortifruti.repository.OAuthCredentialEntryRepository;
import java.io.Serializable;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Substitui o {@code FileDataStoreFactory} da Google API Client Library, que persiste a credencial
 * OAuth num arquivo local (`temp/google/tokens`). Em produção o disco do container é efêmero —
 * redeploy, crash ou um ciclo de sleep/wake apaga esse arquivo, forçando o usuário a reautorizar o
 * Google toda vez que isso acontece (relatado como "preciso autorizar de novo todo dia"). Persistir
 * no MySQL elimina essa dependência do disco local.
 */
@Component
@RequiredArgsConstructor
public class DatabaseDataStoreFactory extends AbstractDataStoreFactory {

  private final OAuthCredentialEntryRepository repository;

  @Override
  protected <V extends Serializable> DataStore<V> createDataStore(String id) {
    return new DatabaseDataStore<>(this, id, repository);
  }

  private static final class DatabaseDataStore<V extends Serializable>
      extends AbstractMemoryDataStore<V> {
    private final OAuthCredentialEntryRepository repository;
    private final String dataStoreId;

    DatabaseDataStore(
        DatabaseDataStoreFactory factory, String id, OAuthCredentialEntryRepository repository) {
      super(factory, id);
      this.repository = repository;
      this.dataStoreId = id;
      repository
          .findAllByDataStoreId(dataStoreId)
          .forEach(entry -> keyValueMap.put(entry.getEntryKey(), entry.getValue()));
    }

    @Override
    public void save() {
      repository.deleteAllByDataStoreId(dataStoreId);
      for (Map.Entry<String, byte[]> entry : keyValueMap.entrySet()) {
        repository.save(
            OAuthCredentialEntry.builder()
                .dataStoreId(dataStoreId)
                .entryKey(entry.getKey())
                .value(entry.getValue())
                .build());
      }
    }
  }
}
