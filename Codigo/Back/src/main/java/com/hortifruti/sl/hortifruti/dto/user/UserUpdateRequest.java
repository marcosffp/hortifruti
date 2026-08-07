package com.hortifruti.sl.hortifruti.dto.user;

import com.hortifruti.sl.hortifruti.model.Role;
import jakarta.validation.constraints.Size;

/**
 * Todos os campos são opcionais — {@code null} (ou em branco, para {@code username}/{@code
 * password}) significa "não alterar", ver {@code UserService#updateUserById}. Por isso não levam
 * {@code @NotNull}/{@code @NotBlank}; só as restrições que fazem sentido quando o valor é
 * de fato enviado.
 */
public record UserUpdateRequest(
    String username,
    @Size(min = 4, max = 20, message = "A senha deve ter entre 4 e 20 caracteres") String password,
    String position,
    Role role) {}
