package com.hortifruti.sl.hortifruti.dto.invoice;

import java.math.BigDecimal;

public record InvoiceResponseGet (
    String name,
    BigDecimal totalValue,
    String status,
    String date,
    String number,
    String reference
) {}
