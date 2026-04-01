package com.hortifruti.sl.hortifruti.dto.purchase;

import com.hortifruti.sl.hortifruti.model.enumeration.Status;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CombinedScoreResponse(
    Long id,
    Long clientId,
    BigDecimal totalValue,
    LocalDate dueDate,
    LocalDate confirmedAt,
    Status status,
    boolean hasBillet,
    boolean hasInvoice,
    String yourNumber,
    String invoiceRef) {}
