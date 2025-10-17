package com.hortifruti.sl.hortifruti.dto.billet;

import java.math.BigDecimal;

public record BilletRequestSimplified(
    String dataEmissao,
    String seuNumero,
    BigDecimal valor,
    String dataVencimento,
    Pagador pagador) {}
