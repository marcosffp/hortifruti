package com.hortifruti.sl.hortifruti.model.googleauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Credencial OAuth2 do Google (Drive/Gmail) persistida no banco em vez de disco local, para
 * sobreviver a reinícios/redeploys de um filesystem efêmero. {@code storeKey} combina o
 * {@code dataStoreId} (fixo, definido pela biblioteca do Google) com a chave do usuário — ver
 * {@link com.hortifruti.sl.hortifruti.service.googleauth.DatabaseDataStore}. O valor em si (um
 * {@code StoredCredential} serializado) fica criptografado — ver
 * {@link com.hortifruti.sl.hortifruti.service.googleauth.TokenEncryptionService}.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "google_oauth_tokens")
public class GoogleOAuthToken {

  @Id
  @Column(name = "store_key", length = 255)
  private String storeKey;

  @Lob
  @Column(name = "encrypted_value", nullable = false)
  private byte[] encryptedValue;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  protected void onSave() {
    this.updatedAt = LocalDateTime.now();
  }
}
