package com.hortifruti.sl.hortifruti.dto.billet;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpenBilletResponse(
    Long combinedScoreId,
    Long clientId,
    String clientName,
    BigDecimal totalValue,
    LocalDate dueDate,
    String yourNumber,
    boolean confirmadoNoSicoob) {}
