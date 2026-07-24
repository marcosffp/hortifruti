package com.hortifruti.sl.hortifruti.dto.bb;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma linha do extrato do BB pronta para exibição (PDF/Excel), no mesmo formato do extrato
 * original: uma linha principal por lançamento (data, agência/lote de origem, documento, histórico,
 * valor, saldo acumulado) e uma sub-linha para o complemento (textoInformacaoComplementar), só com
 * texto em {@code historico}.
 *
 * <p>Diferente do Sicoob, o extrato original do BB mostra o saldo acumulado após cada lançamento
 * (não só por dia) — ver {@link
 * com.hortifruti.sl.hortifruti.service.finance.BBExtratoLayoutService}.
 */
public record BBExtratoLinha(
    LocalDate dataBalancete,
    String agenciaOrigem,
    String lote,
    String documento,
    String historico,
    BigDecimal valor,
    BigDecimal saldo,
    boolean destaque) {}
