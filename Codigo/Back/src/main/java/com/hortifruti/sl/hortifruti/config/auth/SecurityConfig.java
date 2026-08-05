package com.hortifruti.sl.hortifruti.config.auth;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final SecurityFilter securityFilter;
  private final RateLimitingFilter rateLimitingFilter;
  private final DeviceTokenAuthFilter deviceTokenAuthFilter;

  @Value("${frontend.url}")
  private String frontendUrl;

  @Value("${backend.url}")
  private String backendUrl;

  /**
   * Regra de decisão por domínio para esta lista central:
   *
   * <ul>
   *   <li>{@code /auth/**} (login/refresh/logout/me), {@code /swagger-ui/**}/{@code
   *       /v3/api-docs/**} (docs, desabilitadas em prod — ver application-prod.properties), {@code
   *       /backup/oauth2callback} (callback do Google) e {@code
   *       /api/dispositivos/pareamento/confirmar} (o celular ainda não tem cookie/JWT nesse momento
   *       — a segurança do endpoint é o código de pareamento de vida curta e uso único, não uma
   *       sessão) precisam ser públicas por natureza do fluxo, sem alternativa.
   *   <li>{@code /ws/realtime} nunca carrega o cookie {@code auth_token} (conecta direto no domínio
   *       do backend, fora do rewrite same-origin do Next — ver {@code useRealtimeSocket.ts}), então
   *       cai fora do modelo de sessão/role daqui por natureza; quem autentica o handshake é o
   *       {@code AuthHandshakeInterceptor}, trocando um ticket de uso único (ver {@link
   *       com.hortifruti.sl.hortifruti.service.realtime.RealtimeTicketService}) — o próprio ticket só
   *       é emitido para quem já passou pelo catch-all abaixo em {@code /realtime/ws-ticket}.
   *   <li>{@code POST /api/compras/notas/capturas} é o único endpoint que um {@code deviceToken}
   *       (ver {@link DeviceTokenAuthFilter}, autoridade {@code ROLE_DEVICE_CAPTURE}) pode acessar —
   *       precisa de matcher próprio aqui porque a regra abaixo, no catch-all, deliberadamente não
   *       inclui essa role.
   *   <li>Domínios de negócio (produtos, transações, recomendações, notificações) exigem {@code
   *       MANAGER}; leitura de clientes/usuários aceita {@code EMPLOYEE} ou {@code MANAGER}. Regras
   *       mais finas que a role (ex.: mutação x leitura dentro do mesmo domínio) devem usar
   *       {@code @PreAuthorize} no controller, perto do código que protegem, em vez de entrar aqui.
   *   <li>Qualquer rota nova sem matcher explícito cai no catch-all {@code
   *       anyRequest().hasAnyRole("EMPLOYEE", "MANAGER")} — de propósito, não {@code
   *       .authenticated()}: como {@code ROLE_DEVICE_CAPTURE} também é uma autenticação válida
   *       (populada pelo {@link DeviceTokenAuthFilter}), um catch-all baseado só em "autenticado"
   *       deixaria qualquer endpoint sem {@code @PreAuthorize} explícito acessível por um celular
   *       vinculado — inclusive os de outros módulos (boletos, NF-e, financeiro) que hoje contam só
   *       com esta regra central e nenhum {@code @PreAuthorize} próprio. Exigir a role aqui garante
   *       que um {@code deviceToken} nunca alcança nada além do que está explicitamente liberado
   *       acima, mesmo que um controller novo esqueça de declarar sua própria checagem.
   * </ul>
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    SecurityFilterChain chain =
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                auth ->
                    auth.requestMatchers(
                            "/auth",
                            "/auth/logout",
                            "/auth/refresh",
                            "/auth/me",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/backup/oauth2callback",
                            "/api/dispositivos/pareamento/confirmar",
                            "/ws/realtime")
                        .permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/clients/**")
                        .hasAnyRole("EMPLOYEE", "MANAGER")
                        .requestMatchers("/users/**")
                        .hasRole("MANAGER")
                        .requestMatchers("/products/**")
                        .hasRole("MANAGER")
                        .requestMatchers("/api/recommendations/**")
                        .hasRole("MANAGER")
                        .requestMatchers("/api/notifications/test/**")
                        .hasRole("MANAGER")
                        .requestMatchers("/api/notifications/**")
                        .hasAnyRole("EMPLOYEE", "MANAGER")
                        .requestMatchers(
                            org.springframework.http.HttpMethod.POST,
                            "/api/compras/notas/capturas")
                        .hasAnyRole("EMPLOYEE", "MANAGER", "DEVICE_CAPTURE")
                        .anyRequest()
                        .hasAnyRole("EMPLOYEE", "MANAGER"))
            .addFilterBefore(rateLimitingFilter, BasicAuthenticationFilter.class)
            // addFilterBefore(X, Y.class) exige que Y.class já tenha uma posição registrada no
            // momento da chamada (não é resolvido só no .build()) — por isso securityFilter precisa
            // ser registrado (ancorado a um filtro padrão do Spring Security) antes de qualquer
            // outro
            // filtro tentar se ancorar em SecurityFilter.class.
            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(deviceTokenAuthFilter, SecurityFilter.class)
            .build();
    return chain;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    if (frontendUrl == null || backendUrl == null) {
      throw new IllegalStateException(
          "As URLs frontend.url ou backend.url não estão configuradas corretamente.");
    }

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(backendUrl, frontendUrl));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
