package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.dto.user.AuthRequest;
import com.hortifruti.sl.hortifruti.exception.AuthException;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class Auth {

  private final UserRepository userRepository;

  private final PasswordEncoder passwordEncoder;
  private final TokenConfiguration tokenConfiguration;

  public AuthResult autenticar(AuthRequest authRequest) {
    String username = authRequest.username();
    String password = authRequest.password();

    User user = userRepository.findByUsername(username);
    if (user == null) {
      throw new AuthException(
          "Não encontramos um usuário com o nome informado. Verifique e tente novamente.");
    }

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new AuthException("A senha informada está incorreta. Por favor, tente novamente.");
    }

    String token =
        tokenConfiguration.generateToken(user.getId(), user.getUsername(), user.getRole());
    return new AuthResult(token, user);
  }

  public record AuthResult(String token, User user) {}
}
