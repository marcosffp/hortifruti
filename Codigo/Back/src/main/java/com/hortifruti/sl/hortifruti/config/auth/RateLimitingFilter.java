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

  private static final Bandwidth LIMITE_PADRAO =
      Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));

  /**
   * {@code /pareamento/confirmar} é o único endpoint público novo da feature de dispositivo
   * vinculado — sem cookie nem token, só o código de 6 dígitos de vida curta o protege (ver
   * DispositivoVinculadoService) — então recebe um limite bem mais apertado que o padrão de 10/min,
   * pra dificultar força bruta do código.
   */
  private static final Map<String, Bandwidth> LIMITES_POR_ENDPOINT =
      Map.of(
          "/api/dispositivos/pareamento/confirmar",
          Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))));

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
      response.setContentType("application/json");
      response
          .getWriter()
          .write("{\"error\": \"Too many requests to this endpoint. Please try again later.\"}");
    }
  }

  private Bucket createNewBucket(String endpoint) {
    Bandwidth limit = LIMITES_POR_ENDPOINT.getOrDefault(endpoint, LIMITE_PADRAO);
    return Bucket.builder().addLimit(limit).build();
  }
}
