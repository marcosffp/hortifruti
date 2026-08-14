package com.hortifruti.sl.hortifruti.dto.device;

/**
 * Corpo público de {@code POST /api/dispositivos/pareamento/confirmar} — o token em si nunca
 * trafega aqui, só no cookie {@code httpOnly} {@code device_token} (ver {@code
 * DispositivoController}).
 */
public record PareamentoConfirmadoResponse(Long dispositivoId) {}
