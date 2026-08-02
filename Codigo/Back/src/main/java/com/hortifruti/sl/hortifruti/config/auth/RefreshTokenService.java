package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.exception.auth.TokenException;
import com.hortifruti.sl.hortifruti.model.RefreshToken;
import com.hortifruti.sl.hortifruti.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens são rotacionados a cada uso: o token apresentado é revogado e um novo é emitido na
 * mesma chamada. Se um token já revogado for reapresentado (reuso), é sinal de que ele vazou — mas
 * uma reapresentação de um token revogado há poucos segundos é, na prática, quase sempre uma corrida
 * benigna (duas abas, refresh proativo x reativo quase simultâneos, ou a resposta da rotação
 * anterior se perdendo por causa de um restart do backend) — não um ataque. Só fora dessa janela de
 * tolerância é que tratamos como vazamento e revogamos todas as sessões ativas do usuário.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Duration REUSE_GRACE_PERIOD = Duration.ofSeconds(30);

  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${jwt.refresh-expiration-days:30}")
  private long diasExpiracao;

  public String issueToken(Long userId) {
    return persistNewToken(userId);
  }

  @Transactional
  public RotationResult rotate(String rawToken) {
    String hash = hash(rawToken);
    RefreshToken existing =
        refreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(
                () ->
                    new TokenException(
                        "O token de sessão é inválido ou expirou. Por favor, faça login"
                            + " novamente."));

    if (existing.getRevokedAt() != null) {
      boolean possivelCorridaBenigna =
          existing.getRevokedAt().isAfter(LocalDateTime.now().minus(REUSE_GRACE_PERIOD));
      if (!possivelCorridaBenigna) {
        refreshTokenRepository.revokeAllActiveByUserId(existing.getUserId(), LocalDateTime.now());
      }
      throw new TokenException(
          "O token de sessão é inválido ou expirou. Por favor, faça login novamente.");
    }

    if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new TokenException(
          "O token de sessão é inválido ou expirou. Por favor, faça login novamente.");
    }

    existing.setRevokedAt(LocalDateTime.now());
    refreshTokenRepository.save(existing);

    String newRawToken = persistNewToken(existing.getUserId());
    return new RotationResult(existing.getUserId(), newRawToken);
  }

  public void revokeByRawToken(String rawToken) {
    refreshTokenRepository
        .findByTokenHash(hash(rawToken))
        .ifPresent(
            token -> {
              if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
              }
            });
  }

  public long getExpirationSeconds() {
    return diasExpiracao * 24 * 60 * 60;
  }

  private String persistNewToken(Long userId) {
    byte[] randomBytes = new byte[32];
    SECURE_RANDOM.nextBytes(randomBytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

    RefreshToken token =
        RefreshToken.builder()
            .tokenHash(hash(rawToken))
            .userId(userId)
            .expiresAt(LocalDateTime.now().plusDays(diasExpiracao))
            .build();
    refreshTokenRepository.save(token);

    return rawToken;
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 não disponível na JVM.", e);
    }
  }

  public record RotationResult(Long userId, String rawToken) {}
}
