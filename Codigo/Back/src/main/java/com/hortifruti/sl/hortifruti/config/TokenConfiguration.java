package com.hortifruti.sl.hortifruti.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.hortifruti.sl.hortifruti.exception.TokenException;
import com.hortifruti.sl.hortifruti.models.enumeration.Role;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenConfiguration {
  @Value("${jwt.secret}")
  private String secretKey;

  @Value("${jwt.expiration-minutes:60}")
  private long minutosExpiracao;

  private Algorithm algoritmo;

  @PostConstruct
  public void init() {
    algoritmo = Algorithm.HMAC256(secretKey);
  }

  public String generateToken(Long id, String username, Role role) {
    try {
      return JWT.create()
          .withIssuer("auth")
          .withSubject(username)
          .withClaim("id", id)
          .withClaim("role", role.name())
          .withExpiresAt(generateExpirationDate())
          .sign(algoritmo);
    } catch (Exception e) {
      throw new TokenException("Erro ao gerar token JWT", e);
    }
  }

  public String validateToken(String token) {
    try {
      return JWT.require(algoritmo).withIssuer("auth").build().verify(token).getSubject();

    } catch (Exception e) {
      throw new TokenException("Token inválido", e);
    }
  }

  private Instant generateExpirationDate() {
    return LocalDateTime.now().plusMinutes(minutosExpiracao).toInstant(ZoneOffset.of("-03:00"));
  }

  public Role getRoleFromToken(String token) {
    try {
      String roleString =
          JWT.require(algoritmo)
              .withIssuer("auth")
              .build()
              .verify(token)
              .getClaim("role")
              .asString();
      return Role.fromString(roleString);
    } catch (Exception e) {
      throw new TokenException("Token inválido", e);
    }
  }
}
