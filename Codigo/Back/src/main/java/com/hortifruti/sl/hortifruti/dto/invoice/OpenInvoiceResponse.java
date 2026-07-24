package com.hortifruti.sl.hortifruti.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpenInvoiceResponse(
    Long combinedScoreId,
    Long clientId,
    String clientName,
    BigDecimal totalValue,
    LocalDate confirmedAt,
    LocalDate dueDate,
    String invoiceRef) {}
