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
    String clientIp = request.getRemoteAddr();
    String endpoint = request.getRequestURI(); 
    String key =
        clientIp + ":"
            + endpoint; 

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
    Bandwidth limit =
        Bandwidth.classic(
            10, Refill.greedy(10, Duration.ofMinutes(1))); 
    return Bucket.builder().addLimit(limit).build();
  }
}
