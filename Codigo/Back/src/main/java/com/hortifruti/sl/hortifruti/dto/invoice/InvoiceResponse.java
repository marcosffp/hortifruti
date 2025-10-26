package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InvoiceResponse(
    @JsonProperty("ref") String ref, @JsonProperty("status") String status) {}
