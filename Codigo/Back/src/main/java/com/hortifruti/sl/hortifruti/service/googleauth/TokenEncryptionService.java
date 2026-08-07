package com.hortifruti.sl.hortifruti.service.googleauth;

import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Criptografa/descriptografa em repouso o conteúdo salvo em {@code google_oauth_tokens} (AES-GCM).
 * A chave (256 bits, Base64) vem de {@code google.tokens.encryption-key}, mesma família de secret
 * por variável de ambiente usada para {@code jwt.secret}.
 */
@Component
public class TokenEncryptionService {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int GCM_IV_LENGTH_BYTES = 12;

  private final SecretKeySpec secretKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public TokenEncryptionService(@Value("${google.tokens.encryption-key}") String base64Key) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    this.secretKey = new SecretKeySpec(keyBytes, "AES");
  }

  public byte[] encrypt(byte[] plaintext) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext);

      return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
    } catch (GeneralSecurityException e) {
      throw new BackupException("Erro ao criptografar credencial do Google.", e);
    }
  }

  public byte[] decrypt(byte[] encrypted) {
    try {
      ByteBuffer buffer = ByteBuffer.wrap(encrypted);
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      buffer.get(iv);
      byte[] ciphertext = new byte[buffer.remaining()];
      buffer.get(ciphertext);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      return cipher.doFinal(ciphertext);
    } catch (GeneralSecurityException e) {
      throw new BackupException("Erro ao descriptografar credencial do Google.", e);
    }
  }
}
