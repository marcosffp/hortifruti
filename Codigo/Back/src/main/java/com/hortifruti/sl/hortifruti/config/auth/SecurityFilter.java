package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

  private static final Set<String> UNSAFE_METHODS =
      Set.of(
          HttpMethod.POST.name(),
          HttpMethod.PUT.name(),
          HttpMethod.PATCH.name(),
          HttpMethod.DELETE.name());

  private static final Set<String> PUBLIC_AUTH_PATHS =
      Set.of("/auth", "/auth/logout", "/auth/refresh");

  private final TokenConfiguration tokenConfiguration;
  private final UserRepository userRepository;

  @Value("${api.token.scheduler}")
  private String schedulerStaticKey;

  @Value("${frontend.url}")
  private String frontendUrl;

  @Value("${backend.url}")
  private String backendUrl;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (isForgedCrossOriginRequest(request)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response
          .getWriter()
          .write("{\"erro\": \"Acesso negado: origem da requisição não permitida\"}");
      return;
    }

    if (PUBLIC_AUTH_PATHS.contains(request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String token = recoverToken(request);

      if (isSchedulerEndpoint(request.getRequestURI()) && schedulerStaticKey.equals(token)) {
        filterChain.doFilter(request, response);
        return;
      }

      if (token != null) {
        String email = tokenConfiguration.validateToken(token);

        UserDetails user = loadByUserName(email);

        if (user != null) {
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    } catch (Exception e) {
      System.out.println("Erro no filtro de segurança: " + e.getMessage());
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.getWriter().write("{\"erro\": \"Acesso negado: Token inválido ou expirado\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  /**
   * A autenticação é feita por cookie HttpOnly com SameSite=None em produção (front e back estão em
   * domínios diferentes), então o navegador anexa o cookie mesmo em requisições disparadas por
   * outro site. Com CSRF do Spring desabilitado (API stateless), essa checagem de Origin é a defesa
   * contra esse cenário. Requisições sem Origin (clientes não-browser, ex.: scheduler) passam
   * direto, pois não são alvo de CSRF via navegador.
   */
  private boolean isForgedCrossOriginRequest(HttpServletRequest request) {
    if (!UNSAFE_METHODS.contains(request.getMethod())) {
      return false;
    }

    String origin = request.getHeader("Origin");
    if (origin == null || origin.isBlank()) {
      return false;
    }

    return !origin.equals(frontendUrl) && !origin.equals(backendUrl);
  }

  private String recoverToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }

    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("auth_token".equals(cookie.getName()) && !cookie.getValue().isEmpty()) {
          return cookie.getValue();
        }
      }
    }

    return null;
  }

  private UserDetails loadByUserName(String username) {
    return userRepository.findByUsername(username);
  }

  private boolean isSchedulerEndpoint(String uri) {
    return uri.startsWith("/scheduler/health")
        || uri.startsWith("/scheduler/check-overdue")
        || uri.startsWith("/scheduler/check-database-storage");
  }
}
