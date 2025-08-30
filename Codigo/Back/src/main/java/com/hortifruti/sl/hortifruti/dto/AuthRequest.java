package com.hortifruti.sl.hortifruti.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(@NotBlank(message = "nome não pode ser vazio") String username, @NotBlank(message = "senha não pode ser vazia") String password) {}
