package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.dto.user.AuthRequest;
import com.hortifruti.sl.hortifruti.exception.auth.AuthException;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import com.hortifruti.sl.hortifruti.util.HttpRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Auth {

  /**
   * Mensagem idêntica para usuário inexistente, senha incorreta e conta/IP bloqueados — evita
   * enumeration attack (descobrir quais contas existem ou quais estão sob ataque de lockout).
   */
  private static final String GENERIC_AUTH_ERROR_MESSAGE =
      "Credenciais inválidas ou acesso temporariamente restrito. Tente novamente mais tarde.";

  private final UserRepository userRepository;
  private final LoginProtectionService loginProtectionService;

  private final PasswordEncoder passwordEncoder;
  private final TokenConfiguration tokenConfiguration;

  public AuthResult autenticar(AuthRequest authRequest, HttpServletRequest request) {
    String username = authRequest.username();
    String password = authRequest.password();
    String ip = HttpRequestUtils.resolveClientIp(request);
    String userAgent = HttpRequestUtils.resolveUserAgent(request);

    loginProtectionService.assertNotLocked(username, ip, userAgent);

    User user = userRepository.findByUsername(username);
    if (user == null) {
      loginProtectionService.registerFailure(username, ip, userAgent, false);
      throw new AuthException(GENERIC_AUTH_ERROR_MESSAGE);
    }

    if (!passwordEncoder.matches(password, user.getPassword())) {
      loginProtectionService.registerFailure(username, ip, userAgent, true);
      throw new AuthException(GENERIC_AUTH_ERROR_MESSAGE);
    }

    loginProtectionService.registerSuccess(username, ip, userAgent);

    String token =
        tokenConfiguration.generateToken(user.getId(), user.getUsername(), user.getRole());
    return new AuthResult(token, user);
  }

  public record AuthResult(String token, User user) {}
}
