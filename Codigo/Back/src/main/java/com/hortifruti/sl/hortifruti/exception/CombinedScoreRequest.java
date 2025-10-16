package com.hortifruti.sl.hortifruti.exception;

import jakarta.validation.constraints.NotBlank;

public record CombinedScoreRequest(
    @NotBlank(message = "O nome do agrupamento é obrigatório") String name) {}
