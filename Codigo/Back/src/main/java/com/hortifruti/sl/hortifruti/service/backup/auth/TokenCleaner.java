package com.hortifruti.sl.hortifruti.service.backup.auth;

import java.io.File;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TokenCleaner {

  /** Limpa o diretório de tokens */
  protected static void cleanTokenDirectory(String tokensDirectoryPath) {
    java.io.File tokensDir = new java.io.File(tokensDirectoryPath);
    if (tokensDir.exists()) {
      deleteDirectoryRecursively(tokensDir);
    }
  }

  /** Exclui diretório recursivamente */
  private static void deleteDirectoryRecursively(File directory) {
    if (directory.isDirectory()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          deleteDirectoryRecursively(file);
        }
      }
    }
  }
}
