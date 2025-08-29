package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.config.Auth;
import com.hortifruti.sl.hortifruti.dto.AuthRequest;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
  private final Auth auth;

  @PostMapping()
  public ResponseEntity<Map<String, String>> login(@RequestBody AuthRequest authRequest) {
    String token = auth.autenticar(authRequest);
    return ResponseEntity.ok(Map.of("token", token));
  }
}
