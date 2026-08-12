package com.hortifruti.sl.hortifruti.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.model.purchase.StatusCaptura;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Registro em memória das conexões WebSocket abertas por usuário (PC), pra empurrar eventos assim
 * que algo muda no backend — pareamento de dispositivo concluído ({@link
 * com.hortifruti.sl.hortifruti.config.auth.DispositivoVinculadoService}) ou captura de nota mudando
 * de status ({@link com.hortifruti.sl.hortifruti.service.purchase.CapturaExtracaoAsyncService}).
 * Substitui o mecanismo anterior baseado em SSE: WebSocket não sofre do buffering que proxies HTTP
 * (like o rewrite do Next.js) aplicam em streams — a conexão é bidirecional e o navegador reconecta
 * sozinho via {@code useRealtimeSocket} no front, sem depender de nenhum timeout controlado do lado
 * do servidor.
 *
 * <p>Só funciona numa instância única do backend — mesma ressalva já aceita hoje por {@code
 * TokenBlocklist} e {@code RateLimitingFilter}. Se escalar horizontalmente, precisa migrar pra um
 * mecanismo compartilhado (ex.: Redis pub/sub).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeNotificationRegistry {

  private final ObjectMapper objectMapper;

  private final Map<Long, Set<WebSocketSession>> sessoesPorUsuario = new ConcurrentHashMap<>();

  public void registrar(Long usuarioId, WebSocketSession session) {
    sessoesPorUsuario.computeIfAbsent(usuarioId, id -> new CopyOnWriteArraySet<>()).add(session);
  }

  public void remover(Long usuarioId, WebSocketSession session) {
    Set<WebSocketSession> sessoes = sessoesPorUsuario.get(usuarioId);
    if (sessoes == null) {
      return;
    }
    sessoes.remove(session);
    if (sessoes.isEmpty()) {
      sessoesPorUsuario.remove(usuarioId);
    }
  }

  public void notificarDispositivoPareado(Long usuarioId) {
    enviar(usuarioId, new EventoDispositivoPareado());
  }

  public void notificarCapturaAtualizada(Long usuarioId, Long capturaId, StatusCaptura status) {
    enviar(usuarioId, new EventoCapturaAtualizada(capturaId, status));
  }

  /** Melhor esforço: se o PC não tem nenhuma conexão aberta agora, não há nada a notificar. */
  private void enviar(Long usuarioId, Object evento) {
    Set<WebSocketSession> sessoes = sessoesPorUsuario.get(usuarioId);
    if (sessoes == null || sessoes.isEmpty()) {
      return;
    }

    String payload;
    try {
      payload = objectMapper.writeValueAsString(evento);
    } catch (IOException e) {
      log.warn(
          "Falha ao serializar evento realtime para usuarioId={}: {}", usuarioId, e.getMessage());
      return;
    }

    TextMessage mensagem = new TextMessage(payload);
    for (WebSocketSession session : sessoes) {
      try {
        synchronized (session) {
          if (session.isOpen()) {
            session.sendMessage(mensagem);
          }
        }
      } catch (IOException e) {
        log.debug(
            "Removendo sessão WebSocket de usuarioId={} após falha no envio: {}",
            usuarioId,
            e.getMessage());
        remover(usuarioId, session);
      }
    }
  }

  private record EventoDispositivoPareado(String type) {
    EventoDispositivoPareado() {
      this("dispositivo-pareado");
    }
  }

  private record EventoCapturaAtualizada(String type, Long capturaId, StatusCaptura status) {
    EventoCapturaAtualizada(Long capturaId, StatusCaptura status) {
      this("captura-atualizada", capturaId, status);
    }
  }
}
