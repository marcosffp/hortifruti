package com.hortifruti.sl.hortifruti.config.realtime;

import com.hortifruti.sl.hortifruti.service.realtime.RealtimeNotificationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Canal de mão única (servidor → PC): o front nunca envia mensagens por aqui, só escuta eventos
 * ({@code dispositivo-pareado}, {@code captura-atualizada} — ver {@link
 * RealtimeNotificationRegistry}). Por isso não há {@code handleTextMessage} sobrescrito; qualquer
 * mensagem recebida do cliente é ignorada pelo comportamento padrão de {@link
 * TextWebSocketHandler}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

  private final RealtimeNotificationRegistry registry;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    Long usuarioId = usuarioId(session);
    registry.registrar(usuarioId, session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    registry.remover(usuarioId(session), session);
  }

  private Long usuarioId(WebSocketSession session) {
    return (Long) session.getAttributes().get("usuarioId");
  }
}
