package com.hortifruti.sl.hortifruti.service.realtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Ticket de uso único e vida curta para autenticar o handshake do WebSocket de tempo real
 * ({@code /ws/realtime}). Existe porque o cookie {@code auth_token} é emitido para o domínio do
 * front (a chamada de login passa pelo rewrite same-origin do Next, ver {@code authService.ts}),
 * mas o WebSocket conecta direto no domínio do backend — domínios diferentes nunca compartilham
 * cookie, então o handshake nunca teria como reaproveitar a sessão. O front busca um ticket por uma
 * chamada normal autenticada por cookie (same-origin, via {@code /api}) e manda esse ticket na
 * query string do WebSocket; o backend troca o ticket pelo {@code usuarioId} uma única vez.
 *
 * <p>Single-instance deployment only — mesma ressalva já aceita hoje por {@code TokenBlocklist} e
 * {@code RateLimitingFilter}.
 */
@Component
public class RealtimeTicketService {

  private static final Duration TTL = Duration.ofSeconds(15);

  private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

  public String emitir(Long usuarioId) {
    cleanup();
    String ticket = UUID.randomUUID().toString();
    tickets.put(ticket, new Ticket(usuarioId, Instant.now().plus(TTL)));
    return ticket;
  }

  /** Consome o ticket: só pode ser trocado por um usuarioId uma única vez, mesmo se válido. */
  public Optional<Long> consumir(String ticket) {
    if (ticket == null) {
      return Optional.empty();
    }

    Ticket removido = tickets.remove(ticket);
    if (removido == null || removido.expiraEm().isBefore(Instant.now())) {
      return Optional.empty();
    }

    return Optional.of(removido.usuarioId());
  }

  private void cleanup() {
    Instant now = Instant.now();
    tickets.entrySet().removeIf(entry -> entry.getValue().expiraEm().isBefore(now));
  }

  private record Ticket(Long usuarioId, Instant expiraEm) {}
}
