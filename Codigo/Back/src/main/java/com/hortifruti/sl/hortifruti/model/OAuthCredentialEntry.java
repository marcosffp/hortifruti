package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Guarda a credencial OAuth2 do Google (Drive + Gmail) no banco em vez de um arquivo local no
 * disco do container: o disco de produção é efêmero, então qualquer redeploy/restart/ciclo de
 * sleep apagaria o arquivo e forçaria uma nova autorização do usuário. O MySQL sobrevive a esses
 * reinícios. Faz o papel de um {@code com.google.api.client.util.store.DataStore<StoredCredential>}
 * — {@code dataStoreId} é sempre {@code "StoredCredential"} (id fixo da lib) e {@code entryKey} é
 * o userId usado no fluxo OAuth (sempre {@code "user"} neste projeto).
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "oauth_credential_entries",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"data_store_id", "entry_key"}))
public class OAuthCredentialEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "data_store_id", nullable = false, length = 100)
  private String dataStoreId;

  @Column(name = "entry_key", nullable = false, length = 100)
  private String entryKey;

  @Lob
  @Column(name = "value", nullable = false)
  private byte[] value;
}
