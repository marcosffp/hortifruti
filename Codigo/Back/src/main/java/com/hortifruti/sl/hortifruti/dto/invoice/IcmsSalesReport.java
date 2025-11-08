package com.hortifruti.sl.hortifruti.dto.invoice;

import java.math.BigDecimal;
import java.util.Map;

public record IcmsSalesReport(
    BigDecimal totalContabil,
    BigDecimal totalBaseCalculo,
    BigDecimal totalImpostoDebitado,
    BigDecimal totalIsentasOuNaoTributadas,
    BigDecimal totalOutras,
    Map<String, BigDecimal> valoresPorCfop) {}
