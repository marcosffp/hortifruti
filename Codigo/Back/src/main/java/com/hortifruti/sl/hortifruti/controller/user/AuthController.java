package com.hortifruti.sl.hortifruti.controller.user;

import com.hortifruti.sl.hortifruti.config.auth.Auth;
import com.hortifruti.sl.hortifruti.dto.user.AuthRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
  private final Auth auth;

  @PostMapping()
  public ResponseEntity<String> login(@Valid @RequestBody AuthRequest authRequest) {
    String token = auth.autenticar(authRequest);
    return ResponseEntity.ok(token);
  }
}
