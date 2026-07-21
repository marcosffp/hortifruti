package com.hortifruti.sl.hortifruti.dto.sicoob;

import java.util.List;

/**
 * Espelha a resposta 200 de {@code GET /conta-corrente/v4/extrato/{mes}/{ano}} do Sicoob (ver
 * documentacao_sicoob_api.md). Campos com os mesmos nomes do JSON para permitir deserialização
 * direta via Jackson.
 */
public record SicoobExtratoResponse(
    String saldoAtual,
    String saldoBloqueado,
    String saldoLimite,
    String saldoAnterior,
    String saldoBloqueioJudicial,
    String saldoBloqueioJudicialAnterior,
    List<SicoobExtratoTransacao> transacoes) {}
