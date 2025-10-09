package com.hortifruti.sl.hortifruti.dto.user;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank(message = "Por favor, informe o nome de usuário.") String username,
    @NotBlank(message = "Por favor, informe a senha.") String password) {}
