package com.hortifruti.sl.hortifruti.config.realtime;

import com.hortifruti.sl.hortifruti.service.realtime.RealtimeTicketService;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * O handshake de {@code /ws/realtime} não pode reaproveitar o cookie {@code auth_token} da forma
 * como outros endpoints fazem (ver {@code SecurityFilter}): o cookie é emitido para o domínio do
 * front (login passa pelo rewrite same-origin do Next), enquanto o WebSocket conecta direto no
 * domínio do backend — domínios diferentes nunca compartilham cookie, então a autenticação aqui usa
 * um ticket de uso único trocado antes pelo front numa chamada autenticada por cookie (ver {@link
 * RealtimeTicketService} e {@code RealtimeTicketController}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

  private final RealtimeTicketService ticketService;

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    Optional<Long> usuarioId = ticketService.consumir(extrairTicket(request));
    if (usuarioId.isEmpty()) {
      log.warn("Handshake WebSocket recusado: ticket ausente, inválido ou expirado.");
      return false;
    }

    attributes.put("usuarioId", usuarioId.get());
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // Nada a fazer — cleanup de sessão acontece em RealtimeWebSocketHandler#afterConnectionClosed.
  }

  private String extrairTicket(ServerHttpRequest request) {
    if (request instanceof ServletServerHttpRequest servletRequest) {
      return servletRequest.getServletRequest().getParameter("ticket");
    }
    return null;
  }
}
