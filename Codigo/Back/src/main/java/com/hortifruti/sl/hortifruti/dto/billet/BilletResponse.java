package com.hortifruti.sl.hortifruti.dto.billet;

import java.math.BigDecimal;

public record BilletResponse(
    String nomePagador,
    String dataEmissao,
    String dataVencimento,
    String seuNumero,
    String situacaoBoleto,
    String nossoNumero,
    BigDecimal valor) {}
