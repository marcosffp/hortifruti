package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupService {

  private final RefreshTokenRepository refreshTokenRepository;

  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void purgeExpiredTokens() {
    try {
      int removed = refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now());
      if (removed > 0) {
        log.info("Removidos {} refresh tokens expirados", removed);
      }
    } catch (Exception e) {
      log.error("Erro ao limpar refresh tokens expirados: {}", e.getMessage(), e);
    }
  }
}
