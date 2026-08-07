package com.hortifruti.sl.hortifruti.dto.user;

import com.hortifruti.sl.hortifruti.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "Nome de usuário é obrigatório") String username,
    @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 20, message = "A senha deve ter entre 8 e 20 caracteres")
        String password,
    @NotBlank(message = "Cargo é obrigatório") String position,
    @NotNull(message = "Papel é obrigatório") Role role) {}
