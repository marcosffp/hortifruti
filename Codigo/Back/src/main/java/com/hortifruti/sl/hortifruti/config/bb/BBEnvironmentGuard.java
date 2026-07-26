package com.hortifruti.sl.hortifruti.config.bb;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Mesmo raciocínio do {@link com.hortifruti.sl.hortifruti.config.sicoob.SicoobEnvironmentGuard}:
 * fora do profile "prod" (fail-closed: qualquer profile diferente de "prod", incluindo um eventual
 * profile novo/futuro, fica bloqueado por padrão), toda operação que chamaria a API real do Banco
 * do Brasil (saldo, extrato, import) deve ser interceptada e responder com um resultado vazio, sem
 * tocar o BB nem gravar nada localmente. Evita risco de operação financeira real ou vazamento de
 * dados a partir de local/hml.
 */
@Component
@RequiredArgsConstructor
public class BBEnvironmentGuard {

  private final Environment environment;

  public boolean isBlocked() {
    return !environment.matchesProfiles("prod");
  }
}
