package com.hortifruti.sl.hortifruti.model.enumeration;

public enum Role {
  MANAGER,
  EMPLOYEE;

  public static Role fromString(String role) {
    if (role == null || role.trim().isEmpty()) {
      throw new IllegalArgumentException("Tipo de usuário inválido");
    }
    return Role.valueOf(role.toUpperCase());
  }
}
