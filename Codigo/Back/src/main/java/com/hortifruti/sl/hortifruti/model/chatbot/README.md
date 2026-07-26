# com.hortifruti.sl.hortifruti.model.chatbot

Entidade e enums da sessão de conversa do chatbot integrado ao WhatsApp (via Ultramsg).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ChatSession.java` | `@Entity` (`chat_session`) | Sessão de conversa por número de telefone (`phoneNumber`, indexado), referenciando opcionalmente um cliente via `clientId` (FK crua). Guarda `status` (`SessionStatus`, default `MENU`) e `context` (`SessionContext`), ambos `@Enumerated`. Possui `pausedUntil` e os métodos `isPaused()`/`pauseBot(int hours)` para pausar o atendimento automático temporariamente (ex.: quando um humano assume a conversa). |
| `SessionStatus.java` | Enum | Estado da sessão: `MENU`, `AWAITING_DOCUMENT`, `AWAITING_HUMAN`, `PAUSED`, `CLOSED`. |
| `SessionContext.java` | Enum | Assunto/fluxo atual da conversa: `PEDIDO`, `OUTRO`, `BOLETO`, `NOTA_FISCAL`. |
