package com.hortifruti.sl.hortifruti.dto.device;

import java.time.LocalDateTime;

public record DispositivoResponse(
    Long id, String nome, LocalDateTime pareadoEm, LocalDateTime ultimoUsoEm) {}
