package com.hortifruti.sl.hortifruti.service.chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Serviço agendado simplificado para manutenção automática de sessões. Remove apenas pausas
 * expiradas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionCleanupService {

  private final ChatSessionService chatSessionService;

  /** Executa a cada 5 minutos para remover pausas expiradas */
  @Scheduled(fixedRate = 300000) // 5 minutos
  public void unpauseExpiredSessions() {
    try {
      int unpausedCount = chatSessionService.unpauseExpiredSessions();
      if (unpausedCount > 0) {
        log.info("Despausadas {} sessões automaticamente", unpausedCount);
      }
    } catch (Exception e) {
      log.error("Erro ao remover pausas: {}", e.getMessage(), e);
    }
  }
}
