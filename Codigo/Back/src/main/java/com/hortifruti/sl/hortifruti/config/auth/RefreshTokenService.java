package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.exception.auth.TokenException;
import com.hortifruti.sl.hortifruti.model.RefreshToken;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.repository.RefreshTokenRepository;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens são rotacionados a cada uso: o token apresentado é revogado e um novo é emitido na
 * mesma chamada. Se um token já revogado for reapresentado (reuso), é sinal de que ele vazou —
 * todas as sessões ativas do usuário são revogadas como resposta.
 *
 * <p>Exceção deliberada a essa regra: duas chamadas concorrentes de {@code POST /auth/refresh}
 * apresentando o <em>mesmo</em> {@code refresh_token} (duas abas do navegador, várias chamadas de
 * API expirando perto do mesmo instante, um retry) não são reuso malicioso — são a mesma sessão
 * legítima tentando se renovar duas vezes ao mesmo tempo. Sem tratamento especial, a segunda
 * chamada encontraria o token já revogado pela primeira e acionaria a revogação de todas as
 * sessões do usuário, incluindo a que a primeira chamada acabou de emitir com sucesso — ver
 * {@link #handleAlreadyRevoked}.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final String INVALID_TOKEN_MESSAGE =
      "O token de sessão é inválido ou expirou. Por favor, faça login novamente.";

  /**
   * Janela dentro da qual um token "já revogado" reapresentado é tratado como a mesma corrida de
   * rotação concorrente (não como reuso/roubo) — desde que {@link #recentRotations} ainda tenha o
   * registro de para qual token novo ele foi rotacionado (ver {@link #rememberRotation}). Uma
   * janela curta é suficiente para cobrir chamadas concorrentes reais (mesma rede, mesmo instante)
   * sem enfraquecer a detecção de reuso genuíno: um token roubado e reapresentado depois dessa
   * janela (ou sem ter sido a própria rotação que o revogou — ex.: revogado por logout explícito)
   * continua caindo no caminho de revogação total abaixo.
   */
  private static final Duration ROTATION_GRACE_WINDOW = Duration.ofSeconds(10);

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final TokenHasher tokenHasher;

  @Value("${jwt.refresh-expiration-days:30}")
  private long diasExpiracao;

  /**
   * Cache local ao processo (mesma limitação já aceita hoje por {@link TokenBlocklist} e
   * {@link RateLimitingFilter}: single-instance, perdido num restart — o pior efeito de perdê-lo é
   * uma corrida rara voltar a cair no caminho de revogação total, nunca o oposto). Guarda, por
   * hash do token antigo, o token novo (valor bruto, nunca persistido em banco — só o hash é
   * armazenado, ver {@link TokenHasher}) que uma rotação bem-sucedida gerou, só para a janela curta
   * em que uma segunda chamada concorrente com o mesmo token antigo ainda pode aparecer.
   */
  private final Map<String, RecentRotation> recentRotations = new ConcurrentHashMap<>();

  private record RecentRotation(Long userId, String rawToken, Instant expiresAt) {}

  public String issueToken(Long userId) {
    return persistNewToken(userId);
  }

  @Transactional
  public RotationResult rotate(String rawToken) {
    String hash = tokenHasher.hash(rawToken);
    RefreshToken existing =
        refreshTokenRepository
            .findByTokenHashForUpdate(hash)
            .orElseThrow(() -> new TokenException(INVALID_TOKEN_MESSAGE));

    if (existing.getRevokedAt() != null) {
      return handleAlreadyRevoked(hash, existing.getUserId());
    }

    if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new TokenException(INVALID_TOKEN_MESSAGE);
    }

    existing.setRevokedAt(LocalDateTime.now());
    refreshTokenRepository.save(existing);

    User user =
        userRepository
            .findById(existing.getUserId())
            .orElseThrow(() -> new TokenException(INVALID_TOKEN_MESSAGE));

    String newRawToken = persistNewToken(existing.getUserId());
    rememberRotation(hash, existing.getUserId(), newRawToken);
    return new RotationResult(user, newRawToken);
  }

  /**
   * Chamada com a trava pessimista de {@code findByTokenHashForUpdate} ainda em vigor (dentro da
   * mesma transação), então nenhuma outra chamada concorrente para o mesmo token pode estar no
   * meio deste método ao mesmo tempo.
   */
  private RotationResult handleAlreadyRevoked(String hash, Long userId) {
    RecentRotation recent = recentRotations.get(hash);
    if (recent != null && recent.expiresAt().isAfter(Instant.now())) {
      User user =
          userRepository
              .findById(recent.userId())
              .orElseThrow(() -> new TokenException(INVALID_TOKEN_MESSAGE));
      return new RotationResult(user, recent.rawToken());
    }

    refreshTokenRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());
    throw new TokenException(INVALID_TOKEN_MESSAGE);
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

  private void rememberRotation(String oldTokenHash, Long userId, String newRawToken) {
    cleanupRecentRotations();
    recentRotations.put(
        oldTokenHash, new RecentRotation(userId, newRawToken, Instant.now().plus(ROTATION_GRACE_WINDOW)));
  }

  private void cleanupRecentRotations() {
    Instant now = Instant.now();
    recentRotations.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
  }

  public record RotationResult(User user, String rawToken) {}
}
