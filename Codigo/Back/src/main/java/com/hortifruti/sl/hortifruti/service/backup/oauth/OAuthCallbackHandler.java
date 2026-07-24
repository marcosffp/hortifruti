package com.hortifruti.sl.hortifruti.service.backup.oauth;

import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthCallbackHandler {

  private final AuthorizationFlowFactory flowFactory;
  private final TokenProcessor tokenProcessor;

  protected void handleAuthorizationCode(String authorizationCode) {

    try {
      OAuthFlowContext context = flowFactory.createFlowContext();
      tokenProcessor.processAuthorizationCode(authorizationCode, context);

    } catch (Exception e) {
      throw new BackupException("Erro ao processar o callback de autorização.", e);
    }
  }
}
