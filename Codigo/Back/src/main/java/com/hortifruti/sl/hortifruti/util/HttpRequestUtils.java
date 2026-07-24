package com.hortifruti.sl.hortifruti.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A aplicação roda atrás de proxies (Railway e o rewrite same-origin do Next.js), então
 * request.getRemoteAddr() sempre retorna o IP do proxy — usar isso como chave faria todos os
 * usuários dividirem o mesmo balde de rate limit / contador de lockout. X-Forwarded-For carrega o
 * IP original do cliente, adicionado pelo proxy de borda; pegamos o primeiro valor da lista (o
 * cliente mais próximo da origem).
 */
public final class HttpRequestUtils {

  private HttpRequestUtils() {}

  public static String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  public static String resolveUserAgent(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    return (userAgent == null || userAgent.isBlank()) ? "desconhecido" : userAgent;
  }
}
