package com.hortifruti.sl.hortifruti.service.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiTokenService {

  @Value("${api.token.scheduler}")
  private String schedulerToken;

  public boolean validateSchedulerToken(String token) {
    return schedulerToken != null && schedulerToken.equals(token);
  }
}
