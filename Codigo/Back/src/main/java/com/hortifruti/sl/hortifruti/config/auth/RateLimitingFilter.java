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
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
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

  /**
   * TTL bem acima da maior janela de refill (1 min) — uma entrada só é descartada quando já está
   * ociosa havia tempo suficiente para o bucket ter voltado a ficar cheio de qualquer forma, então
   * recriá-la do zero na próxima requisição não relaxa o limite de ninguém.
   */
  private static final Duration BUCKET_TTL = Duration.ofMinutes(10);

  private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();

  private record BucketEntry(Bucket bucket, AtomicReference<Instant> lastAccess) {
    static BucketEntry of(Bucket bucket) {
      return new BucketEntry(bucket, new AtomicReference<>(Instant.now()));
    }
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String clientIp = HttpRequestUtils.resolveClientIp(request);
    String endpoint = request.getRequestURI();
    String key = clientIp + ":" + endpoint;

    BucketEntry entry = buckets.computeIfAbsent(key, k -> BucketEntry.of(createNewBucket(endpoint)));
    entry.lastAccess().set(Instant.now());

    if (entry.bucket().tryConsume(1)) {
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

  @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
  void evictStaleBuckets() {
    Instant cutoff = Instant.now().minus(BUCKET_TTL);
    int before = buckets.size();
    buckets.values().removeIf(entry -> entry.lastAccess().get().isBefore(cutoff));
    int removed = before - buckets.size();
    if (removed > 0) {
      log.debug("Removidos {} buckets de rate limit ociosos por mais de {}", removed, BUCKET_TTL);
    }
  }
}
