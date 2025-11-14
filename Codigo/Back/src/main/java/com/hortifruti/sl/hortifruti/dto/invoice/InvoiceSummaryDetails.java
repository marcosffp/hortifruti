package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record InvoiceSummaryDetails(
    @JsonProperty("especie") String especie,
    @JsonProperty("serie") String serie,
    @JsonProperty("dia") String dia,
    @JsonProperty("uf") String uf,
    @JsonProperty("valor") BigDecimal valor,
    @JsonProperty("predominante") String predominante,
    @JsonProperty("aliquota") BigDecimal aliquota) {}
