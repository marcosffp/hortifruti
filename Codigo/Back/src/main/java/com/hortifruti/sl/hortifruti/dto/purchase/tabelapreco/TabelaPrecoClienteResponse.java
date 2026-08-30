package com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TabelaPrecoClienteResponse(
    Long id,
    Long clienteId,
    int competenciaMes,
    int competenciaAno,
    LocalDate vigenciaInicio,
    LocalDate vigenciaFim,
    int versao,
    StatusTabelaPreco status,
    LocalDateTime importadoEm,
    LocalDateTime confirmadoEm,
    List<TabelaPrecoClienteItemResponse> itens) {}
