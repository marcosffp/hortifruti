package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.dto.AuthRequest;
import com.hortifruti.sl.hortifruti.exception.AuthException;
import com.hortifruti.sl.hortifruti.models.User;
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

  public String autenticar(AuthRequest authRequest) {
    String username = authRequest.username();
    String password = authRequest.password();

    System.out.println("Nome de usuário recebido: " + username);

    User user = userRepository.findByUsername(username);
    if (user == null) {
      System.out.println("Usuário não encontrado: " + username);
      throw new AuthException("Usuário não encontrado.");
    }

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new AuthException("Senha incorreta para usuário.");
    }

    return tokenConfiguration.generateToken(user.getId(), user.getUsername(), user.getRole());
  }
}
