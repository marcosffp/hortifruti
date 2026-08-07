package com.hortifruti.sl.hortifruti.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A aplicação roda atrás de proxies (Railway e o rewrite same-origin do Next.js), então
 * request.getRemoteAddr() sempre retorna o IP do proxy — usar isso como chave faria todos os
 * usuários dividirem o mesmo balde de rate limit / contador de lockout. X-Forwarded-For é enviado
 * pelo cliente e pode ser forjado à vontade; cada proxy confiável pelo qual a requisição passa
 * *acrescenta* o IP que enxergou ao final da cadeia, então só o último valor da lista foi escrito
 * por infraestrutura nossa (Railway) — os valores anteriores podem ser qualquer coisa que o
 * cliente tenha mandado. Pegamos por isso o último valor, nunca o primeiro.
 */
public final class HttpRequestUtils {

  private HttpRequestUtils() {}

  public static String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      String[] parts = forwardedFor.split(",");
      return parts[parts.length - 1].trim();
    }
    return request.getRemoteAddr();
  }

  public static String resolveUserAgent(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    return (userAgent == null || userAgent.isBlank()) ? "desconhecido" : userAgent;
  }
}
