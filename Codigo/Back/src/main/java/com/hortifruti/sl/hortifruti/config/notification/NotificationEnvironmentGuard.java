package com.hortifruti.sl.hortifruti.config.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Em homologação (profile "hml") o banco costuma ter clientes reais copiados de produção, e o
 * remetente de e-mail configurado é uma conta real (não uma sandbox) — bloqueia o envio de
 * notificações (e-mail/WhatsApp) para clientes ou contabilidade nesse ambiente, evitando que testes
 * cheguem a destinatários reais.
 */
@Component
@RequiredArgsConstructor
public class NotificationEnvironmentGuard {

  private final Environment environment;

  public boolean isBlocked() {
    return environment.matchesProfiles("hml");
  }
}
