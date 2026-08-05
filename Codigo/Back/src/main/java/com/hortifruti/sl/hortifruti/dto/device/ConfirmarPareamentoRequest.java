package com.hortifruti.sl.hortifruti.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmarPareamentoRequest(
    @NotBlank(message = "Por favor, informe o código de pareamento.")
        @Pattern(regexp = "\\d{6}", message = "Código de pareamento inválido.")
        String codigo,
    @Size(max = 100, message = "O nome do dispositivo deve ter no máximo 100 caracteres.")
        String nomeDispositivo) {}
