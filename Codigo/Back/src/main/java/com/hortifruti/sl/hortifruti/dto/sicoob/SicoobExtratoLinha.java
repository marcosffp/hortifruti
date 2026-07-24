package com.hortifruti.sl.hortifruti.dto.sicoob;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma linha do extrato pronto para exibição (PDF/Excel), no mesmo formato do extrato original do
 * Sicoob: uma linha principal por transação, sub-linhas para o complemento (descInfComplementar,
 * separado por "|@"), e linhas de "SALDO DO DIA"/"SALDO ANTERIOR".
 *
 * <p>{@code data}, {@code documento} e {@code valor} vêm nulos nas sub-linhas de complemento (só
 * têm texto em {@code historico}).
 */
public record SicoobExtratoLinha(
    LocalDate data, String documento, String historico, BigDecimal valor, boolean destaque) {}
