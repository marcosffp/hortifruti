# com.hortifruti.sl.hortifruti.repository.chatbot

Repositório das sessões de conversa do chatbot de WhatsApp, usado para controlar estado, fila de atendimento humano e pausas de sessão.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `ChatSessionRepository.java` | `JpaRepository<ChatSession, Long>` | Entidade `ChatSession`. `findFirstByPhoneNumberOrderByCreatedAtDesc` busca a sessão mais recente de um telefone; `findSessionsAwaitingHuman` (`@Query`) lista sessões com status `AWAITING_HUMAN` ordenadas por criação; `findSessionsToUnpause` (`@Query`) lista sessões cujo `pausedUntil` já expirou; `countActiveSessionsByClientId` (`@Query`) conta sessões de um cliente; `findByStatusOrderByCreatedAtDesc(SessionStatus)` lista sessões por status. |
