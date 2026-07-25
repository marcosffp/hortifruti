package com.hortifruti.sl.hortifruti.config.sicoob;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * O Sicoob não oferece ambiente de homologação/sandbox — só existe a conta real de produção.
 * Fora do profile "prod" (fail-closed: qualquer profile diferente de "prod", incluindo um
 * eventual profile novo/futuro, fica bloqueado por padrão), toda operação que chamaria a API real
 * do Sicoob deve ser interceptada e responder com um resultado vazio, sem tocar o Sicoob nem
 * gravar nada localmente. Evita risco de operação financeira real ou vazamento de dados a partir
 * de local/hml.
 */
@Component
@RequiredArgsConstructor
public class SicoobEnvironmentGuard {

  private final Environment environment;

  public boolean isBlocked() {
    return !environment.matchesProfiles("prod");
  }
}
