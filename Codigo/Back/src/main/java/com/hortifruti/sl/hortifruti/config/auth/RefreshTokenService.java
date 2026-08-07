package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.exception.auth.TokenException;
import com.hortifruti.sl.hortifruti.model.RefreshToken;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.repository.RefreshTokenRepository;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens são rotacionados a cada uso: o token apresentado é revogado e um novo é emitido na
 * mesma chamada. Se um token já revogado for reapresentado (reuso), é sinal de que ele vazou —
 * todas as sessões ativas do usuário são revogadas como resposta.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final TokenHasher tokenHasher;

  @Value("${jwt.refresh-expiration-days:30}")
  private long diasExpiracao;

  public String issueToken(Long userId) {
    return persistNewToken(userId);
  }

  @Transactional
  public RotationResult rotate(String rawToken) {
    String hash = tokenHasher.hash(rawToken);
    RefreshToken existing =
        refreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(
                () ->
                    new TokenException(
                        "O token de sessão é inválido ou expirou. Por favor, faça login"
                            + " novamente."));

    if (existing.getRevokedAt() != null) {
      refreshTokenRepository.revokeAllActiveByUserId(existing.getUserId(), LocalDateTime.now());
      throw new TokenException(
          "O token de sessão é inválido ou expirou. Por favor, faça login novamente.");
    }

    if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new TokenException(
          "O token de sessão é inválido ou expirou. Por favor, faça login novamente.");
    }

    existing.setRevokedAt(LocalDateTime.now());
    refreshTokenRepository.save(existing);

    User user =
        userRepository
            .findById(existing.getUserId())
            .orElseThrow(
                () ->
                    new TokenException(
                        "O token de sessão é inválido ou expirou. Por favor, faça login"
                            + " novamente."));

    String newRawToken = persistNewToken(existing.getUserId());
    return new RotationResult(user, newRawToken);
  }

  public void revokeByRawToken(String rawToken) {
    refreshTokenRepository
        .findByTokenHash(tokenHasher.hash(rawToken))
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
    String rawToken = tokenHasher.generateOpaqueToken();

    RefreshToken token =
        RefreshToken.builder()
            .tokenHash(tokenHasher.hash(rawToken))
            .userId(userId)
            .expiresAt(LocalDateTime.now().plusDays(diasExpiracao))
            .build();
    refreshTokenRepository.save(token);

    return rawToken;
  }

  public record RotationResult(User user, String rawToken) {}
}
