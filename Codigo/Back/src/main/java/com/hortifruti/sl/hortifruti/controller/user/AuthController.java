package com.hortifruti.sl.hortifruti.controller.user;

import com.hortifruti.sl.hortifruti.config.auth.Auth;
import com.hortifruti.sl.hortifruti.config.auth.Auth.AuthResult;
import com.hortifruti.sl.hortifruti.config.auth.TokenConfiguration;
import com.hortifruti.sl.hortifruti.dto.user.AuthRequest;
import com.hortifruti.sl.hortifruti.dto.user.AuthUserResponse;
import com.hortifruti.sl.hortifruti.model.User;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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

  private final Auth auth;
  private final TokenConfiguration tokenConfiguration;

  @Value("${auth.cookie.secure:false}")
  private boolean cookieSecure;

  @Value("${auth.cookie.samesite:Lax}")
  private String cookieSameSite;

  @PostMapping()
  public ResponseEntity<AuthUserResponse> login(@Valid @RequestBody AuthRequest authRequest) {
    AuthResult result = auth.autenticar(authRequest);

    ResponseCookie cookie =
        buildCookie(result.token(), tokenConfiguration.getExpirationSeconds());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(toResponse(result.user()));
  }

  @GetMapping("/me")
  public ResponseEntity<AuthUserResponse> me() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return ResponseEntity.ok(toResponse(user));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    ResponseCookie cookie = buildCookie("", 0);
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
  }

  private ResponseCookie buildCookie(String token, long maxAgeSeconds) {
    return ResponseCookie.from(COOKIE_NAME, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite(cookieSameSite)
        .path("/")
        .maxAge(maxAgeSeconds)
        .build();
  }

  private AuthUserResponse toResponse(User user) {
    return new AuthUserResponse(
        user.getId(), user.getUsername(), user.getUsername(), List.of(user.getRole().name()));
  }
}
