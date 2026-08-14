package com.hortifruti.sl.hortifruti.controller.device;

import com.hortifruti.sl.hortifruti.config.auth.DispositivoVinculadoService;
import com.hortifruti.sl.hortifruti.dto.device.ConfirmarPareamentoRequest;
import com.hortifruti.sl.hortifruti.dto.device.ConfirmarPareamentoResponse;
import com.hortifruti.sl.hortifruti.dto.device.DispositivoResponse;
import com.hortifruti.sl.hortifruti.dto.device.IniciarPareamentoResponse;
import com.hortifruti.sl.hortifruti.dto.device.PareamentoConfirmadoResponse;
import com.hortifruti.sl.hortifruti.dto.device.PareamentoStatusResponse;
import com.hortifruti.sl.hortifruti.model.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

  private static final String DEVICE_COOKIE_NAME = "device_token";

  private final DispositivoVinculadoService dispositivoVinculadoService;

  @Value("${auth.cookie.secure:false}")
  private boolean cookieSecure;

  @Value("${auth.cookie.samesite:Lax}")
  private String cookieSameSite;

  /**
   * Mesmo TTL de inatividade usado pelo backend pra revogar automaticamente (ver {@code
   * DispositivoVinculadoService#inatividadeMaxDias}) — o cookie não precisa durar mais que isso, já
   * que o token seria rejeitado de qualquer forma depois desse prazo sem uso.
   */
  @Value("${dispositivo.inatividade-max-dias:90}")
  private long inatividadeMaxDias;

  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @PostMapping("/pareamento/iniciar")
  public ResponseEntity<IniciarPareamentoResponse> iniciarPareamento() {
    return ResponseEntity.ok(
        dispositivoVinculadoService.gerarCodigoPareamento(usuarioAutenticadoId()));
  }

  /**
   * Público: quem chama é o navegador do celular, que ainda não tem nenhum credencial nesse momento
   * — a segurança deste endpoint é o código de 6 dígitos de vida curta e uso único, não uma sessão
   * (liberado explicitamente em SecurityConfig). O device token gerado nunca volta no corpo da
   * resposta — só como cookie {@code httpOnly}, pra não ficar exposto a XSS via `localStorage` (ver
   * Área C, item C-V5 da auditoria de frontend).
   */
  @PostMapping("/pareamento/confirmar")
  public ResponseEntity<PareamentoConfirmadoResponse> confirmarPareamento(
      @Valid @RequestBody ConfirmarPareamentoRequest request) {
    ConfirmarPareamentoResponse resultado =
        dispositivoVinculadoService.confirmarPareamento(
            request.codigo(), request.nomeDispositivo());

    ResponseCookie deviceCookie =
        buildDeviceCookie(resultado.deviceToken(), inatividadeMaxDias * 24 * 60 * 60);

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, deviceCookie.toString())
        .body(new PareamentoConfirmadoResponse(resultado.dispositivoId()));
  }

  /**
   * Público, sem efeito no backend além de limpar o cookie do celular que está chamando — a
   * revogação "de verdade" (que invalida o token pros outros também) é {@link #revogar}, só
   * acessível do PC autenticado. Existe porque o cookie é {@code httpOnly}: o JS do celular não
   * tem como apagá-lo sozinho pra, por exemplo, permitir vincular com outro código.
   */
  @PostMapping("/pareamento/desvincular")
  public ResponseEntity<Void> desvincularLocal() {
    ResponseCookie cleared = buildDeviceCookie("", 0);
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
  }

  /**
   * Público: usado pela página de vínculo pra saber, ao carregar, se o celular já tem um device
   * token válido — o cookie sendo {@code httpOnly}, o JS não consegue checar isso sozinho (mesmo
   * padrão de {@code GET /auth/me} pro cookie de sessão).
   */
  @GetMapping("/pareamento/status")
  public ResponseEntity<PareamentoStatusResponse> status(HttpServletRequest request) {
    String token = recoverDeviceCookie(request);
    boolean pareado = token != null && dispositivoVinculadoService.tokenValido(token);
    return ResponseEntity.ok(new PareamentoStatusResponse(pareado));
  }

  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @GetMapping
  public ResponseEntity<List<DispositivoResponse>> listar() {
    return ResponseEntity.ok(
        dispositivoVinculadoService.listarDispositivos(usuarioAutenticadoId()));
  }

  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> revogar(@PathVariable Long id) {
    dispositivoVinculadoService.revogar(id, usuarioAutenticadoId());
    return ResponseEntity.noContent().build();
  }

  private Long usuarioAutenticadoId() {
    User usuario = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return usuario.getId();
  }

  private String recoverDeviceCookie(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    for (Cookie cookie : request.getCookies()) {
      if (DEVICE_COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isEmpty()) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private ResponseCookie buildDeviceCookie(String token, long maxAgeSeconds) {
    return ResponseCookie.from(DEVICE_COOKIE_NAME, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite(cookieSameSite)
        .path("/")
        .maxAge(maxAgeSeconds)
        .build();
  }
}
