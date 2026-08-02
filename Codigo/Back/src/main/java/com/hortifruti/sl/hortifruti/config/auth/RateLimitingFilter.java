package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.util.HttpRequestUtils;
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

  private static final Bandwidth DEFAULT_LIMIT =
      Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));

  /**
   * /auth/me é uma checagem de sessão sem efeito colateral, disparada pelo front a cada navegação
   * (AuthGuard/useAuth) — sem relação com força bruta, então recebe um limite bem mais folgado que
   * os demais endpoints. Sem isso, navegação ativa normal esgota o limite padrão e o front interpreta
   * o 429 resultante como "sessão expirada", derrubando o usuário para o login.
   */
  private static final Map<String, Bandwidth> ENDPOINT_LIMITS =
      Map.of("/auth/me", Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1))));

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String clientIp = HttpRequestUtils.resolveClientIp(request);
    String endpoint = request.getRequestURI();
    String key = clientIp + ":" + endpoint;

    Bucket bucket = buckets.computeIfAbsent(key, k -> createNewBucket(endpoint));

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      response.setStatus(429);
      response
          .getWriter()
          .write("{\"error\": \"Too many requests to this endpoint. Please try again later.\"}");
    }
  }

  private Bucket createNewBucket(String endpoint) {
    Bandwidth limit = ENDPOINT_LIMITS.getOrDefault(endpoint, DEFAULT_LIMIT);
    return Bucket.builder().addLimit(limit).build();
  }
}
