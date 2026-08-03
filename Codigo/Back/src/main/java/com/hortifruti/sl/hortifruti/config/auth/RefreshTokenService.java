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
 * anterior se perdendo por causa de um restart do backend) — não um ataque. Nesse caso seguimos a
 * cadeia de rotação até o token vigente e rotacionamos a partir dele, devolvendo um par de cookies
 * válido para a chamada perdedora em vez de derrubar a sessão. Só fora dessa janela de tolerância
 * (ou quando a cadeia não resolve) é que tratamos como vazamento e revogamos todas as sessões ativas
 * do usuário.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Duration REUSE_GRACE_PERIOD = Duration.ofSeconds(30);
  private static final int MAX_CHAIN_HOPS = 5;
  private static final String INVALID_TOKEN_MESSAGE =
      "O token de sessão é inválido ou expirou. Por favor, faça login novamente.";

  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${jwt.refresh-expiration-days:30}")
  private long diasExpiracao;

  public String issueToken(Long userId) {
    return persistNewToken(userId, null);
  }

  @Transactional
  public RotationResult rotate(String rawToken) {
    String hash = hash(rawToken);
    RefreshToken presented =
        refreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new TokenException(INVALID_TOKEN_MESSAGE));

    if (presented.getRevokedAt() != null) {
      return rotateFromRevoked(presented);
    }

    if (presented.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new TokenException(INVALID_TOKEN_MESSAGE);
    }

    return rotateValid(presented);
  }

  private RotationResult rotateFromRevoked(RefreshToken presented) {
    boolean possivelCorridaBenigna =
        presented.getRevokedAt().isAfter(LocalDateTime.now().minus(REUSE_GRACE_PERIOD));

    if (possivelCorridaBenigna) {
      RefreshToken tokenVigente = followChainToActiveToken(presented);
      if (tokenVigente != null) {
        return rotateValid(tokenVigente);
      }
    } else {
      refreshTokenRepository.revokeAllActiveByUserId(presented.getUserId(), LocalDateTime.now());
    }

    throw new TokenException(INVALID_TOKEN_MESSAGE);
  }

  /**
   * Segue {@code replacedByHash} a partir de um token já revogado até achar o token vigente da
   * cadeia (não revogado e ainda não expirado). Devolve {@code null} se a cadeia terminar num token
   * inexistente/expirado ou passar de {@link #MAX_CHAIN_HOPS} saltos — nesses casos não há uma
   * sessão vigente pra recuperar, então a chamada é mesmo inválida.
   */
  private RefreshToken followChainToActiveToken(RefreshToken token) {
    RefreshToken current = token;
    for (int hop = 0; hop < MAX_CHAIN_HOPS; hop++) {
      String nextHash = current.getReplacedByHash();
      if (nextHash == null) return null;

      RefreshToken next = refreshTokenRepository.findByTokenHash(nextHash).orElse(null);
      if (next == null) return null;

      if (next.getRevokedAt() == null) {
        return next.getExpiresAt().isAfter(LocalDateTime.now()) ? next : null;
      }
      current = next;
    }
    return null;
  }

  private RotationResult rotateValid(RefreshToken token) {
    String newRawToken = persistNewToken(token.getUserId(), token);
    return new RotationResult(token.getUserId(), newRawToken);
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

  private String persistNewToken(Long userId, RefreshToken tokenSendoSubstituido) {
    byte[] randomBytes = new byte[32];
    SECURE_RANDOM.nextBytes(randomBytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    String newHash = hash(rawToken);

    RefreshToken token =
        RefreshToken.builder()
            .tokenHash(newHash)
            .userId(userId)
            .expiresAt(LocalDateTime.now().plusDays(diasExpiracao))
            .build();
    refreshTokenRepository.save(token);

    if (tokenSendoSubstituido != null) {
      tokenSendoSubstituido.setRevokedAt(LocalDateTime.now());
      tokenSendoSubstituido.setReplacedByHash(newHash);
      refreshTokenRepository.save(tokenSendoSubstituido);
    }

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
