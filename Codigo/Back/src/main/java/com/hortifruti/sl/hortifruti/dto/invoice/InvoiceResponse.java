package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceResponse(
    @JsonProperty("ref") String ref, @JsonProperty("status") String status) {}
