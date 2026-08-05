package com.hortifruti.sl.hortifruti.dto.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.StatusCaptura;
import java.time.LocalDateTime;

public record CapturaPendenteResponse(
    Long id,
    StatusCaptura status,
    NotaExtracaoResponse extracao,
    String mensagemErro,
    LocalDateTime criadaEm) {}
