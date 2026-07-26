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

  @Value("${frontend.url}")
  private String frontendUrl;

  @Value("${backend.url}")
  private String backendUrl;

  /**
   * Regra de decisão por domínio para esta lista central:
   *
   * <ul>
   *   <li>{@code /auth/**} (login/refresh/logout/me), {@code /swagger-ui/**}/{@code
   *       /v3/api-docs/**} (docs, desabilitadas em prod — ver application-prod.properties) e {@code
   *       /backup/oauth2callback} (callback do Google) precisam ser públicas por natureza do fluxo,
   *       sem alternativa.
   *   <li>Domínios de negócio (produtos, transações, recomendações, notificações) exigem {@code
   *       MANAGER}; leitura de clientes/usuários aceita {@code EMPLOYEE} ou {@code MANAGER}. Regras
   *       mais finas que a role (ex.: mutação x leitura dentro do mesmo domínio) devem usar
   *       {@code @PreAuthorize} no controller, perto do código que protegem, em vez de entrar aqui.
   *   <li>Qualquer rota nova sem matcher explícito cai em {@code anyRequest().authenticated()} — ou
   *       seja, qualquer usuário autenticado (independente do papel) consegue acessá-la a menos que
   *       o controller declare {@code @PreAuthorize} explicitamente.
   * </ul>
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                        "/backup/oauth2callback")
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
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(rateLimitingFilter, BasicAuthenticationFilter.class)
        .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
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
