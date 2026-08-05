package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica requisições de um dispositivo vinculado (celular que já fez o pareamento — ver {@link
 * DispositivoVinculadoService}) via um header dedicado, {@code X-Device-Token}, propositalmente
 * separado do {@code Authorization} usado pelo JWT de login: se os dois dividissem o mesmo header,
 * o {@link SecurityFilter} (que já rejeita a requisição inteira quando o Bearer não decodifica como
 * JWT válido) entraria em conflito com este filtro sobre quem decide primeiro. Com um header
 * próprio, os dois fluxos de autenticação nunca colidem — este filtro só age quando {@code
 * X-Device-Token} está presente; do contrário, é transparente e o {@link SecurityFilter} segue seu
 * fluxo normal de JWT/cookie sem nenhuma interferência.
 *
 * <p>A autoridade concedida ({@code ROLE_DEVICE_CAPTURE}) é montada manualmente — nunca a partir de
 * {@code user.getAuthorities()} — e só é reconhecida pelos endpoints de captura de nota (ver
 * {@code @PreAuthorize} em {@code NotaController}). Um dispositivo vinculado nunca tem acesso
 * equivalente ao login completo do usuário, mesmo que o usuário seja {@code MANAGER}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTokenAuthFilter extends OncePerRequestFilter {

  private static final String HEADER_NAME = "X-Device-Token";

  /**
   * Limite por dispositivo, não por IP — o {@code RateLimitingFilter} genérico já protege por IP,
   * mas cada chamada de captura consome cota do free tier do Gemini, então um único dispositivo
   * comprometido não deveria conseguir esgotar essa cota sozinho, mesmo vindo de um IP
   * compartilhado com outros dispositivos legítimos (ex.: Wi-Fi da loja).
   */
  private static final Bandwidth LIMITE_POR_DISPOSITIVO =
      Bandwidth.classic(15, Refill.greedy(15, Duration.ofMinutes(1)));

  private final DispositivoVinculadoService dispositivoVinculadoService;
  private final UserRepository userRepository;
  private final Map<Long, Bucket> bucketsPorDispositivo = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String deviceToken = request.getHeader(HEADER_NAME);

    if (deviceToken != null && !deviceToken.isBlank()) {
      DispositivoVinculadoService.DispositivoAutenticado autenticado;
      try {
        autenticado = dispositivoVinculadoService.validarToken(deviceToken);
      } catch (Exception e) {
        log.warn(
            "Token de dispositivo inválido em {}: {}", request.getRequestURI(), e.getMessage());
        responderErro(
            response,
            HttpServletResponse.SC_UNAUTHORIZED,
            "Dispositivo não vinculado ou token inválido.");
        return;
      }

      if (!consumirCotaDoDispositivo(autenticado.dispositivoId())) {
        responderErro(response, 429, "Muitas requisições deste dispositivo. Aguarde um minuto.");
        return;
      }

      User usuario = userRepository.findById(autenticado.usuarioId()).orElse(null);
      if (usuario == null) {
        responderErro(
            response,
            HttpServletResponse.SC_UNAUTHORIZED,
            "Dispositivo não vinculado ou token inválido.");
        return;
      }

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              usuario, null, List.of(new SimpleGrantedAuthority("ROLE_DEVICE_CAPTURE")));
      authentication.setDetails(autenticado.dispositivoId());
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private boolean consumirCotaDoDispositivo(Long dispositivoId) {
    Bucket bucket =
        bucketsPorDispositivo.computeIfAbsent(
            dispositivoId, id -> Bucket.builder().addLimit(LIMITE_POR_DISPOSITIVO).build());
    return bucket.tryConsume(1);
  }

  private void responderErro(HttpServletResponse response, int status, String mensagem)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.getWriter().write("{\"erro\": \"" + mensagem + "\"}");
  }
}
