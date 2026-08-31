package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.exception.auth.TokenException;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
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

  /**
   * {@link SecurityContextHolder#getContext()} sozinho só vale para a thread/dispatch atual. Em
   * endpoints assíncronos ({@code CompletableFuture}, ex.: {@code POST
   * /invoices/issue-with-billet/{id}}, que espera até ~2min pela NF-e antes de responder), o Spring
   * reentra na cadeia de filtros num REDISPATCH separado quando o resultado fica pronto — e o
   * {@code SecurityContextHolderFilter} do Spring Security (diferente deste filtro, que é {@code
   * OncePerRequestFilter} e por isso pula os redispatches) roda de novo nesse redispatch e
   * recarrega o contexto do zero a partir de um {@link SecurityContextRepository}. Sem salvar aqui,
   * esse redispatch encontra um contexto vazio (mesmo com o JWT válido) e a requisição é tratada
   * como anônima → 403 só nesse endpoint, só depois da espera longa. Salvar explicitamente no mesmo
   * repositório padrão do Spring Security ({@link RequestAttributeSecurityContextRepository}, que
   * guarda como atributo do {@code HttpServletRequest} em vez de ThreadLocal) resolve isso.
   */
  private final SecurityContextRepository securityContextRepository =
      new RequestAttributeSecurityContextRepository();

  private final TokenConfiguration tokenConfiguration;
  private final UserRepository userRepository;

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

      if (token != null) {
        String email = tokenConfiguration.validateToken(token);

        UserDetails user = loadByUserName(email);

        if (user != null) {
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

          SecurityContext context = SecurityContextHolder.createEmptyContext();
          context.setAuthentication(authentication);
          SecurityContextHolder.setContext(context);
          securityContextRepository.saveContext(context, request, response);

          if (user instanceof User appUser
              && appUser.isMustChangePassword()
              && !isPasswordChangeAllowedPath(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response
                .getWriter()
                .write(
                    "{\"erro\": \"PASSWORD_CHANGE_REQUIRED\", \"mensagem\": \"Troque sua senha"
                        + " temporária antes de continuar.\"}");
            return;
          }
        }
      }
    } catch (TokenException e) {
      // 401, não 403: token ausente/inválido/expirado é "não autenticado", não "sem permissão" —
      // ver o porquê em TokenException.getHttpStatus(). É o sinal que o cliente usa pra saber que
      // vale a pena tentar POST /auth/refresh antes de desistir e mandar pro login.
      log.warn("Erro no filtro de segurança para {}: {}", request.getRequestURI(), e.getMessage());
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("{\"erro\": \"Sessão expirada ou token inválido\"}");
      return;
    } catch (Exception e) {
      // Diferente de TokenException acima: aqui o problema não é o token em si, e sim algo que
      // impediu validá-lo (ex.: instabilidade momentânea de conexão com o banco ao consultar o
      // usuário/blocklist). Devolver 403 nesse caso faria o front tratar como sessão expirada e
      // mandar o usuário pra tela de login — quando na verdade é só tentar de novo em instantes.
      log.error(
          "Falha inesperada no filtro de segurança para {} — não é problema de token",
          request.getRequestURI(),
          e);
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      response
          .getWriter()
          .write(
              "{\"erro\": \"Erro temporário ao validar sua sessão. Tente novamente em"
                  + " instantes.\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Enquanto {@code mustChangePassword} for {@code true}, só {@code GET /auth/me} (para o front
   * saber que precisa redirecionar) e {@code PUT /users} (a própria troca de senha) ficam
   * acessíveis — qualquer outra rota autenticada é bloqueada até a senha temporária ser trocada.
   */
  private boolean isPasswordChangeAllowedPath(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if ("/auth/me".equals(uri)) {
      return true;
    }
    return "/users".equals(uri) && HttpMethod.PUT.matches(request.getMethod());
  }

  /**
   * A autenticação é feita por cookie HttpOnly com SameSite=None em produção (front e back estão em
   * domínios diferentes), então o navegador anexa o cookie mesmo em requisições disparadas por
   * outro site. Com CSRF do Spring desabilitado (API stateless), essa checagem de Origin é a defesa
   * contra esse cenário. Requisições sem Origin (clientes não-browser, ex.: webhook da UltraMsg)
   * passam direto, pois não são alvo de CSRF via navegador.
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
}
