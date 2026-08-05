package com.hortifruti.sl.hortifruti.config.realtime;

import com.hortifruti.sl.hortifruti.model.User;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * O handshake de {@code /ws/realtime} é uma requisição HTTP normal antes de virar upgrade — passa
 * pela mesma cadeia de filtros do Spring Security que qualquer outro endpoint (ver {@code
 * SecurityFilter}, que já validou o cookie {@code auth_token} e populou o {@code
 * SecurityContextHolder} nesse mesmo request/thread antes deste interceptor rodar), então não há
 * necessidade de duplicar a leitura/validação do JWT aqui: só reaproveita a autenticação já feita e
 * guarda o {@code usuarioId} nos atributos da sessão pro {@link RealtimeWebSocketHandler} usar.
 */
@Slf4j
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

  private static final Set<String> PAPEIS_PERMITIDOS = Set.of("ROLE_MANAGER", "ROLE_EMPLOYEE");

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication != null && authentication.getPrincipal() instanceof User usuario)
        || !temPapelPermitido(authentication)) {
      log.warn("Handshake WebSocket recusado: usuário não autenticado ou sem papel permitido.");
      return false;
    }

    attributes.put("usuarioId", usuario.getId());
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

  private boolean temPapelPermitido(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(PAPEIS_PERMITIDOS::contains);
  }
}
