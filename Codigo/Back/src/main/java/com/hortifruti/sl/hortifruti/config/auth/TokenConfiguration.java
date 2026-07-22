package com.hortifruti.sl.hortifruti.config.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hortifruti.sl.hortifruti.exception.TokenException;
import com.hortifruti.sl.hortifruti.model.enumeration.Role;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenConfiguration {
  private final TokenBlocklist tokenBlocklist;

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
          .withClaim("role", "ROLE_" + role.name())
          .withExpiresAt(generateExpirationDate())
          .sign(algoritmo);
    } catch (Exception e) {
      throw new TokenException(
          "Ocorreu um erro ao gerar o token de acesso. Por favor, tente novamente mais tarde.", e);
    }
  }

  public String validateToken(String token) {
    if (tokenBlocklist.isRevoked(token)) {
      throw new TokenException(
          "O token fornecido é inválido ou expirou. Por favor, faça login novamente.");
    }

    try {
      return JWT.require(algoritmo).withIssuer("auth").build().verify(token).getSubject();

    } catch (Exception e) {
      throw new TokenException(
          "O token fornecido é inválido ou expirou. Por favor, faça login novamente.", e);
    }
  }

  /** Revoga o token imediatamente, mesmo que ele ainda não tenha expirado (usado no logout). */
  public void revokeToken(String token) {
    try {
      DecodedJWT decoded = JWT.require(algoritmo).withIssuer("auth").build().verify(token);
      tokenBlocklist.revoke(token, decoded.getExpiresAtAsInstant());
    } catch (Exception e) {
      // Token já inválido/expirado: nada a revogar.
    }
  }

  private Instant generateExpirationDate() {
    return LocalDateTime.now().plusMinutes(minutosExpiracao).toInstant(ZoneOffset.of("-03:00"));
  }

  public long getExpirationSeconds() {
    return minutosExpiracao * 60;
  }
}
