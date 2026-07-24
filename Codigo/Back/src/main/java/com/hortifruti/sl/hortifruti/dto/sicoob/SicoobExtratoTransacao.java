package com.hortifruti.sl.hortifruti.dto.sicoob;

/**
 * Espelha um item de {@code transacoes[]} da resposta de {@code GET
 * /conta-corrente/v4/extrato/{mes}/{ano}} do Sicoob (ver documentacao_sicoob_api.md). Campos com os
 * mesmos nomes do JSON para permitir deserialização direta via Jackson.
 */
public record SicoobExtratoTransacao(
    String transactionId,
    String tipo,
    String valor,
    String data,
    String dataLote,
    String descricao,
    String numeroDocumento,
    String cpfCnpj,
    String descInfComplementar) {}
