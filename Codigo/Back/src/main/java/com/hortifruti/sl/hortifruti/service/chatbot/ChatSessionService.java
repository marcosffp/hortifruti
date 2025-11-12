package com.hortifruti.sl.hortifruti.service.chatbot;

import com.hortifruti.sl.hortifruti.model.chatbot.ChatSession;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionContext;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionStatus;
import com.hortifruti.sl.hortifruti.repository.chatbot.ChatSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para gerenciar sessões de chat e seu ciclo de vida. (Simplificado: sem armazenamento de
 * mensagens)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

  private final ChatSessionRepository sessionRepository;

  /** Obtém ou cria uma nova sessão para um número de telefone */
  @Transactional
  public ChatSession getOrCreateSession(String phoneNumber) {
    Optional<ChatSession> existingSession =
        sessionRepository.findActiveSessionByPhoneNumber(phoneNumber);

    return existingSession.orElseGet(() -> createNewSession(phoneNumber));
  }

  /** Cria uma nova sessão de chat */
  @Transactional
  public ChatSession createNewSession(String phoneNumber) {
    return sessionRepository.save(
        ChatSession.builder().phoneNumber(phoneNumber).status(SessionStatus.MENU).build());
  }

  /** Atualiza o status da sessão */
  @Transactional
  public ChatSession updateSessionStatus(Long sessionId, SessionStatus newStatus) {
    ChatSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));

    session.setStatus(newStatus);
    return sessionRepository.save(session);
  }

  /** Define o contexto da sessão */
  @Transactional
  public ChatSession setSessionContext(Long sessionId, SessionContext context) {
    ChatSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));

    session.setContext(context);
    return sessionRepository.save(session);
  }

  /** Associa um cliente à sessão */
  @Transactional
  public ChatSession associateClient(Long sessionId, Long clientId) {
    ChatSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));

    session.setClientId(clientId);
    return sessionRepository.save(session);
  }

  /** Pausa o bot para uma sessão (após resposta manual do atendente) */
  @Transactional
  public ChatSession pauseBotForSession(Long sessionId, int hours) {
    ChatSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));

    session.pauseBot(hours);
    log.info("Bot pausado para sessão {} até {}", sessionId, session.getPausedUntil());
    return sessionRepository.save(session);
  }

  /** Pausa o bot para um número de telefone */
  @Transactional
  public void pauseBotForPhone(String phoneNumber, int hours) {
    Optional<ChatSession> sessionOpt =
        sessionRepository.findActiveSessionByPhoneNumber(phoneNumber);

    if (sessionOpt.isPresent()) {
      ChatSession session = sessionOpt.get();
      session.pauseBot(hours);
      sessionRepository.save(session);
      log.info("Bot pausado para telefone {} até {}", phoneNumber, session.getPausedUntil());
    }
  }

  /** Remove a pausa do bot */
  @Transactional
  public ChatSession unpauseBot(Long sessionId) {
    ChatSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));

    session.setPausedUntil(null);
    return sessionRepository.save(session);
  }

  /** Fecha uma sessão (remove do sistema) */
  @Transactional
  public void closeSession(Long sessionId, String reason) {
    log.info("Fechando sessão {}. Motivo: {}", sessionId, reason);
    sessionRepository.deleteById(sessionId);
  }

  /** Verifica se o bot está pausado para um telefone */
  public boolean isBotPausedForPhone(String phoneNumber) {
    Optional<ChatSession> sessionOpt =
        sessionRepository.findActiveSessionByPhoneNumber(phoneNumber);

    return sessionOpt.map(ChatSession::isPaused).orElse(false);
  }

  /** Busca sessões aguardando atendimento humano */
  public List<ChatSession> getSessionsAwaitingHuman() {
    return sessionRepository.findSessionsAwaitingHuman();
  }

  /** Remove pausas expiradas automaticamente e volta status para MENU */
  @Transactional
  public int unpauseExpiredSessions() {
    List<ChatSession> sessionsToUnpause =
        sessionRepository.findSessionsToUnpause(LocalDateTime.now());

    for (ChatSession session : sessionsToUnpause) {
      session.setPausedUntil(null);

      // Se o status era PAUSED, volta para MENU após o atendimento humano
      if (session.getStatus() == SessionStatus.PAUSED) {
        session.setStatus(SessionStatus.MENU);
        log.info("Sessão {} despausada e status mudado para MENU", session.getId());
      }

      sessionRepository.save(session);
    }

    if (!sessionsToUnpause.isEmpty()) {
      log.info("Removida pausa de {} sessões", sessionsToUnpause.size());
    }

    return sessionsToUnpause.size();
  }
}
