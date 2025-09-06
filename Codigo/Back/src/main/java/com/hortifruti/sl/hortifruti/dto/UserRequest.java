package com.hortifruti.sl.hortifruti.dto;

import com.hortifruti.sl.hortifruti.model.enumeration.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
    String username,
    @NotBlank(message = "Por favor, informe uma senha.")
        @Size(min = 4, max = 20, message = "A senha deve ter entre 4 e 20 caracteres.")
        String password,
    @NotNull(message = "Por favor, selecione o papel do usuário.") Role role) {}
