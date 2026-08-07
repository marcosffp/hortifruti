package com.hortifruti.sl.hortifruti.config.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Geração e hashing de tokens opacos (refresh token, token de dispositivo vinculado): 32 bytes via
 * {@link SecureRandom} codificados em Base64 URL-safe sem padding para o token em si, e SHA-256 em
 * hex para o valor persistido no banco (nunca o token em claro). Compartilhado entre {@link
 * RefreshTokenService} e {@link DispositivoVinculadoService} — antes cada um reimplementava a
 * mesma lógica de forma independente; centralizar aqui garante que uma futura migração de
 * algoritmo (ex.: Argon2/PBKDF2) só precise mudar em um lugar.
 */
@Component
public class TokenHasher {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;

  public String generateOpaqueToken() {
    byte[] randomBytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  public String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 não disponível na JVM.", e);
    }
  }
}
