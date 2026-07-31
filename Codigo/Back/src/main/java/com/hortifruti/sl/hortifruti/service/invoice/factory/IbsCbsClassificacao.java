package com.hortifruti.sl.hortifruti.service.invoice.factory;

import java.math.BigDecimal;

/**
 * Classificação tributária de um item para IBS/CBS.
 *
 * <p>{@code percentualReducaoAliquota} é o percentual de redução (tag Sefaz {@code pRedAliq}, ex.:
 * 60 para hortifrúti/ovos) previsto para o {@code cClassTrib}. As alíquotas nominais de CBS/IBS UF
 * (tags {@code pCBS}/{@code pIBSUF}) NÃO variam por item — durante a fase de testes de 2026 elas
 * são fixas (0,9% / 0,1%) mesmo para itens com redução; é o percentual de redução, aplicado sobre
 * essa alíquota nominal, que produz a alíquota efetiva (tag {@code pAliqEfet}) usada no cálculo do
 * valor do imposto.
 */
public record IbsCbsClassificacao(
    String situacaoTributaria,
    String classificacaoTributaria,
    BigDecimal percentualReducaoAliquota) {}
