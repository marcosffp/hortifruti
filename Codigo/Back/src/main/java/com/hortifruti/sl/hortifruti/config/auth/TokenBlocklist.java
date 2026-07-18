package com.hortifruti.sl.hortifruti.config.auth;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory denylist of revoked JWTs, used so /auth/logout actually invalidates a token instead of
 * relying only on the client discarding its cookie. Single-instance deployment only — entries are
 * lost on restart, which just means a revoked token becomes valid again until it naturally expires.
 */
@Component
public class TokenBlocklist {
  private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

  public void revoke(String token, Instant expiresAt) {
    cleanup();
    revokedTokens.put(token, expiresAt);
  }

  public boolean isRevoked(String token) {
    Instant expiresAt = revokedTokens.get(token);
    return expiresAt != null && expiresAt.isAfter(Instant.now());
  }

  private void cleanup() {
    Instant now = Instant.now();
    revokedTokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
  }
}
