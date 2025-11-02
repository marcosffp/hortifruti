package com.hortifruti.sl.hortifruti.service.backup.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.javanet.NetHttpTransport;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthFlowContext {
  private final NetHttpTransport httpTransport;
  private final GoogleAuthorizationCodeFlow flow;
}
