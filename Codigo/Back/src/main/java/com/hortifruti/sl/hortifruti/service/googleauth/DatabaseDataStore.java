package com.hortifruti.sl.hortifruti.service.googleauth;

import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.model.googleauth.GoogleOAuthToken;
import com.hortifruti.sl.hortifruti.repository.googleauth.GoogleOAuthTokenRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link DataStore} do google-oauth-client com persistência em banco (via {@link
 * GoogleOAuthTokenRepository}) em vez de arquivo local — tokens sobrevivem a reinícios/redeploys de
 * um filesystem efêmero. Cada entrada fica com o valor serializado (Java {@link Serializable}, como
 * {@code StoredCredential}) e criptografado em repouso pelo {@link TokenEncryptionService}.
 */
class DatabaseDataStore<V extends Serializable> extends AbstractDataStore<V> {

  private final GoogleOAuthTokenRepository repository;
  private final TokenEncryptionService encryptionService;
  private final String keyPrefix;

  protected DatabaseDataStore(
      DataStoreFactory dataStoreFactory,
      String id,
      GoogleOAuthTokenRepository repository,
      TokenEncryptionService encryptionService) {
    super(dataStoreFactory, id);
    this.repository = repository;
    this.encryptionService = encryptionService;
    this.keyPrefix = id + "::";
  }

  @Override
  public Set<String> keySet() {
    return repository.findAllByStoreKeyStartingWith(keyPrefix).stream()
        .map(entity -> entity.getStoreKey().substring(keyPrefix.length()))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<V> values() {
    return repository.findAllByStoreKeyStartingWith(keyPrefix).stream()
        .map(entity -> this.<V>deserialize(encryptionService.decrypt(entity.getEncryptedValue())))
        .collect(Collectors.toList());
  }

  @Override
  public V get(String key) {
    if (key == null) {
      return null;
    }
    return repository
        .findById(toStoreKey(key))
        .map(entity -> this.<V>deserialize(encryptionService.decrypt(entity.getEncryptedValue())))
        .orElse(null);
  }

  @Override
  public DataStore<V> set(String key, V value) {
    GoogleOAuthToken entity =
        repository
            .findById(toStoreKey(key))
            .orElseGet(() -> GoogleOAuthToken.builder().storeKey(toStoreKey(key)).build());
    entity.setEncryptedValue(encryptionService.encrypt(serialize(value)));
    repository.save(entity);
    return this;
  }

  @Override
  public DataStore<V> clear() {
    repository.deleteAllByStoreKeyStartingWith(keyPrefix);
    return this;
  }

  @Override
  public DataStore<V> delete(String key) {
    repository.deleteById(toStoreKey(key));
    return this;
  }

  private String toStoreKey(String key) {
    return keyPrefix + key;
  }

  private byte[] serialize(V value) {
    try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
      objectStream.writeObject(value);
      objectStream.flush();
      return byteStream.toByteArray();
    } catch (IOException e) {
      throw new BackupException("Erro ao serializar credencial do Google.", e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T deserialize(byte[] data) {
    try (ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(data))) {
      return (T) objectStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new BackupException("Erro ao desserializar credencial do Google.", e);
    }
  }
}
