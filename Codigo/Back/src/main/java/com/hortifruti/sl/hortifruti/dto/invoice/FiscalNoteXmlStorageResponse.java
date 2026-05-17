package com.hortifruti.sl.hortifruti.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FiscalNoteXmlStorageResponse(
    Long id,
    String ref,
    String nfNumber,
    String clientName,
    BigDecimal totalValue,
    LocalDate issuedAt,
    LocalDateTime createdAt) {}
