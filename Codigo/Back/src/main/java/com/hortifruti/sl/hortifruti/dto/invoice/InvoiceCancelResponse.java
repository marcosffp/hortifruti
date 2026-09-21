package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resposta do cancelamento de NF-e. {@code status} é {@code "CANCELADO"} quando a Focus
 * NFe/SEFAZ já confirmou o cancelamento na própria chamada, ou {@code "PROCESSANDO"} quando o
 * cancelamento foi aceito mas ainda depende de confirmação assíncrona da SEFAZ (ver {@code
 * FiscalNoteCancellationPoller}) — o front não deve tratar {@code "PROCESSANDO"} como
 * cancelamento definitivo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceCancelResponse(String ref, String status, String message) {}
