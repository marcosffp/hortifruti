package com.hortifruti.sl.hortifruti.service.backup.oauth;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthCallbackHandler {

  private final AuthorizationFlowFactory flowFactory;
  private final TokenProcessor tokenProcessor;

  /** Processa o código de autorização recebido do Google. */
  protected void handleAuthorizationCode(String authorizationCode) {

    try {
      OAuthFlowContext context = flowFactory.createFlowContext();
      tokenProcessor.processAuthorizationCode(authorizationCode, context);

    } catch (Exception e) {
      throw new BackupException("Erro ao processar o callback de autorização.", e);
    }
  }
}
