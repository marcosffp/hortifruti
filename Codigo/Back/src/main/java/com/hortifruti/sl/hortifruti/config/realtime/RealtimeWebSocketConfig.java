package com.hortifruti.sl.hortifruti.config.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RealtimeWebSocketConfig implements WebSocketConfigurer {

  private final RealtimeWebSocketHandler realtimeWebSocketHandler;
  private final AuthHandshakeInterceptor authHandshakeInterceptor;

  @Value("${frontend.url}")
  private String frontendUrl;

  @Value("${backend.url}")
  private String backendUrl;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(realtimeWebSocketHandler, "/ws/realtime")
        .addInterceptors(authHandshakeInterceptor)
        .setAllowedOrigins(frontendUrl, backendUrl);
  }
}
