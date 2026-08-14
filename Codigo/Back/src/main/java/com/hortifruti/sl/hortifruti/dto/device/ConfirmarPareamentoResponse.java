package com.hortifruti.sl.hortifruti.dto.device;

/**
 * Retorno interno de {@code DispositivoVinculadoService#confirmarPareamento} — {@code deviceToken}
 * nunca é serializado de volta pro celular no corpo da resposta HTTP, só usado pelo controller pra
 * montar o cookie {@code httpOnly} (ver {@code DispositivoController}). O corpo público da
 * resposta é {@code PareamentoConfirmadoResponse}.
 */
public record ConfirmarPareamentoResponse(String deviceToken, Long dispositivoId) {}
