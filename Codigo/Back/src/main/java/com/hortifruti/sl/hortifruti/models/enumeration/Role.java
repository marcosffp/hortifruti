package com.hortifruti.sl.hortifruti.models.enumeration;

public enum Role {
  GESTOR,
  FUNCIONARIO;

  public static Role fromString(String role) {
    if (role == null || role.trim().isEmpty()) {
      throw new IllegalArgumentException("Tipo de usuário inválido");
    }
    return Role.valueOf(role.toUpperCase());
  }
}
