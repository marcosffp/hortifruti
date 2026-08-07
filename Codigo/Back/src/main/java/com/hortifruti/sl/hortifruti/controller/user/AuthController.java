package com.hortifruti.sl.hortifruti.controller.user;

import com.hortifruti.sl.hortifruti.config.auth.Auth;
import com.hortifruti.sl.hortifruti.config.auth.Auth.AuthResult;
import com.hortifruti.sl.hortifruti.config.auth.RefreshTokenService;
import com.hortifruti.sl.hortifruti.config.auth.RefreshTokenService.RotationResult;
import com.hortifruti.sl.hortifruti.config.auth.TokenConfiguration;
import com.hortifruti.sl.hortifruti.dto.user.AuthRequest;
import com.hortifruti.sl.hortifruti.dto.user.AuthUserResponse;
import com.hortifruti.sl.hortifruti.exception.auth.TokenException;
import com.hortifruti.sl.hortifruti.model.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  private static final String COOKIE_NAME = "auth_token";
  private static final String REFRESH_COOKIE_NAME = "refresh_token";

  private final Auth auth;
  private final TokenConfiguration tokenConfiguration;
  private final RefreshTokenService refreshTokenService;
  private final Environment environment;

  @Value("${auth.cookie.secure:false}")
  private boolean cookieSecure;

  @Value("${auth.cookie.samesite:Lax}")
  private String cookieSameSite;

  @PostMapping()
  public ResponseEntity<AuthUserResponse> login(
      @Valid @RequestBody AuthRequest authRequest, HttpServletRequest request) {
    AuthResult result = auth.autenticar(authRequest, request);
    String refreshToken = refreshTokenService.issueToken(result.user().getId());

    ResponseCookie accessCookie =
        buildCookie(COOKIE_NAME, result.token(), tokenConfiguration.getExpirationSeconds(), "/");
    ResponseCookie refreshCookie =
        buildCookie(
            REFRESH_COOKIE_NAME, refreshToken, refreshTokenService.getExpirationSeconds(), "/auth");

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(toResponse(result.user()));
  }

  @GetMapping("/me")
  public ResponseEntity<AuthUserResponse> me() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication != null && authentication.getPrincipal() instanceof User user)) {
      return ResponseEntity.ok(null);
    }

    return ResponseEntity.ok(toResponse(user));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthUserResponse> refresh(HttpServletRequest request) {
    String refreshToken = recoverRefreshToken(request);
    if (refreshToken == null) {
      return clearedCookiesResponse();
    }

    RotationResult rotation;
    try {
      rotation = refreshTokenService.rotate(refreshToken);
    } catch (TokenException e) {
      return clearedCookiesResponse();
    }

    User user = rotation.user();

    String accessToken =
        tokenConfiguration.generateToken(user.getId(), user.getUsername(), user.getRole());

    ResponseCookie accessCookie =
        buildCookie(COOKIE_NAME, accessToken, tokenConfiguration.getExpirationSeconds(), "/");
    ResponseCookie refreshCookie =
        buildCookie(
            REFRESH_COOKIE_NAME,
            rotation.rawToken(),
            refreshTokenService.getExpirationSeconds(),
            "/auth");

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(toResponse(user));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    String token = recoverToken(request);
    if (token != null) {
      tokenConfiguration.revokeToken(token);
    }

    String refreshToken = recoverRefreshToken(request);
    if (refreshToken != null) {
      refreshTokenService.revokeByRawToken(refreshToken);
    }

    ResponseCookie accessCookie = buildCookie(COOKIE_NAME, "", 0, "/");
    ResponseCookie refreshCookie = buildCookie(REFRESH_COOKIE_NAME, "", 0, "/auth");

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .build();
  }

  private ResponseEntity<AuthUserResponse> clearedCookiesResponse() {
    ResponseCookie accessCookie = buildCookie(COOKIE_NAME, "", 0, "/");
    ResponseCookie refreshCookie = buildCookie(REFRESH_COOKIE_NAME, "", 0, "/auth");

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .build();
  }

  private String recoverToken(HttpServletRequest request) {
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }

    return recoverCookie(request, COOKIE_NAME);
  }

  private String recoverRefreshToken(HttpServletRequest request) {
    return recoverCookie(request, REFRESH_COOKIE_NAME);
  }

  private String recoverCookie(HttpServletRequest request, String name) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (name.equals(cookie.getName()) && !cookie.getValue().isEmpty()) {
          return cookie.getValue();
        }
      }
    }

    return null;
  }

  private ResponseCookie buildCookie(String name, String token, long maxAgeSeconds, String path) {
    return ResponseCookie.from(name, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite(cookieSameSite)
        .path(path)
        .maxAge(maxAgeSeconds)
        .build();
  }

  private AuthUserResponse toResponse(User user) {
    return new AuthUserResponse(
        user.getId(),
        user.getUsername(),
        user.getUsername(),
        List.of(user.getRole().name()),
        activeEnvironment());
  }

  /**
   * Exposto ao front para esconder/bloquear funcionalidades sensíveis (ex.: envio de notificação
   * por e-mail real) quando rodando contra o ambiente de homologação.
   */
  private String activeEnvironment() {
    String[] profiles = environment.getActiveProfiles();
    return profiles.length > 0 ? profiles[0] : "default";
  }
}
