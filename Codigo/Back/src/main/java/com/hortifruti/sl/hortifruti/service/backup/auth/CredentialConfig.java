package com.hortifruti.sl.hortifruti.service.backup.auth;

import java.io.File;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CredentialConfig {
  private final String applicationName;
  private final String tokensDirectoryPath;
  private final String redirectUri;
  private final File credentialsFile;
}
