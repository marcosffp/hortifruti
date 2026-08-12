package com.hortifruti.sl.hortifruti.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code PUT /users} — troca da própria senha temporária (ver {@code
 * SecurityFilter#isPasswordChangeAllowedPath}). Diferente de {@link UserUpdateRequest}, aqui {@code
 * username} e {@code password} são obrigatórios: {@code username} identifica o usuário (não há
 * {@code @PathVariable} nesta rota) e {@code password} é exatamente o dado que a operação existe
 * para alterar — "não informado = não altera" não faz sentido para nenhum dos dois campos aqui.
 */
public record ChangeOwnPasswordRequest(
    @NotBlank(message = "Username é obrigatório") String username,
    @NotBlank(message = "Senha é obrigatória")
        @Size(min = 4, max = 20, message = "A senha deve ter entre 4 e 20 caracteres")
        String password) {}
