package com.hortifruti.sl.hortifruti.repository.chatbot;

import com.hortifruti.sl.hortifruti.model.chatbot.ChatSession;
import com.hortifruti.sl.hortifruti.model.chatbot.SessionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository para gerenciar sessões de chat. */
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

  /**
   * Busca a sessão ativa mais recente de um número de telefone (simplificado: qualquer sessão
   * existente é ativa, sessões concluídas são deletadas)
   */
  @Query(
      "SELECT cs FROM ChatSession cs WHERE cs.phoneNumber = :phoneNumber "
          + "ORDER BY cs.createdAt DESC")
  Optional<ChatSession> findActiveSessionByPhoneNumber(@Param("phoneNumber") String phoneNumber);

  /** Busca sessões aguardando atendimento humano */
  @Query(
      "SELECT cs FROM ChatSession cs WHERE cs.status = 'AWAITING_HUMAN' "
          + "ORDER BY cs.createdAt ASC")
  List<ChatSession> findSessionsAwaitingHuman();

  /** Busca sessões pausadas que já podem ser reativadas */
  @Query(
      "SELECT cs FROM ChatSession cs WHERE cs.pausedUntil IS NOT NULL "
          + "AND cs.pausedUntil < :now")
  List<ChatSession> findSessionsToUnpause(@Param("now") LocalDateTime now);

  /**
   * Conta quantas sessões ativas um cliente possui (simplificado: todas as sessões existentes são
   * ativas)
   */
  @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.clientId = :clientId")
  long countActiveSessionsByClientId(@Param("clientId") Long clientId);

  /** Busca sessões por status (simplificado: ordena por createdAt ao invés de updatedAt) */
  List<ChatSession> findByStatusOrderByCreatedAtDesc(SessionStatus status);
}
