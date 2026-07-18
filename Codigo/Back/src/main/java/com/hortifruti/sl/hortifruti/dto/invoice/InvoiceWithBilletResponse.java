package com.hortifruti.sl.hortifruti.dto.invoice;

public record InvoiceWithBilletResponse(
    String invoiceRef,
    String invoiceNumber,
    String billetNumber,
    String danfeBase64,
    String xmlBase64,
    String billetBase64) {}
