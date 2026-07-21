package com.hortifruti.sl.hortifruti.dto.sicoob;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumo de uma busca de extrato via API do Sicoob. Quando {@code alreadyProcessed} é {@code true},
 * o período pedido já tinha sido buscado antes — a API do Sicoob não foi chamada de novo, e os
 * campos de totais vêm zerados.
 */
public record SicoobImportSummary(
    Long statementId,
    boolean alreadyProcessed,
    LocalDate periodStart,
    LocalDate periodEnd,
    int totalFetched,
    int totalSaved,
    int totalDuplicatedSkipped,
    BigDecimal totalEntradas,
    BigDecimal totalSaidas) {}
