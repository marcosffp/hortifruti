package com.hortifruti.sl.hortifruti.config.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String clientIp = resolveClientIp(request);
    String endpoint = request.getRequestURI();
    String key = clientIp + ":" + endpoint;

    Bucket bucket = buckets.computeIfAbsent(key, this::createNewBucket);

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      response.setStatus(429);
      response
          .getWriter()
          .write("{\"error\": \"Too many requests to this endpoint. Please try again later.\"}");
    }
  }

  private Bucket createNewBucket(String key) {
    Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
    return Bucket.builder().addLimit(limit).build();
  }

  /**
   * A aplicação roda atrás de proxies (Railway e o rewrite same-origin do Next.js), então
   * request.getRemoteAddr() sempre retorna o IP do proxy — usar isso como chave faria todos os
   * usuários dividirem o mesmo balde de rate limit. X-Forwarded-For carrega o IP original do
   * cliente, adicionado pelo proxy de borda; pegamos o primeiro valor da lista (o cliente mais
   * próximo da origem).
   */
  private String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
