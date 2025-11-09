package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

  private final TokenConfiguration tokenConfiguration;
  private final UserRepository userRepository;

  @Value("${api.token.scheduler}")
  private String schedulerStaticKey;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
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

  private String recoverToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    return (authHeader != null && authHeader.startsWith("Bearer "))
        ? authHeader.substring(7)
        : null;
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
