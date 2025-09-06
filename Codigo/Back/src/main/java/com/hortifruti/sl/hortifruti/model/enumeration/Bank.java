package com.hortifruti.sl.hortifruti.model.enumeration;

import java.util.Arrays;

public enum Bank {
  BANCO_DO_BRASIL,
  SICOOB,
  UNKNOWN;

  public static Bank parseBank(String bank) {
    if (bank == null) return UNKNOWN;
    String normalized = normalize(bank);

    if (normalized.matches("^(BB|BANCODOBRASIL|BANCOBRASIL).*$")) {
      return BANCO_DO_BRASIL;
    }
    if (normalized.contains("SICOOB")) {
      return SICOOB;
    }
    return Arrays.stream(Bank.values())
        .filter(b -> normalized.equals(normalize(b.name())))
        .findFirst()
        .orElse(UNKNOWN);
  }

  private static String normalize(String value) {
    return value.trim().toUpperCase().replaceAll("[\\s_\\-.]+", "");
  }
}
