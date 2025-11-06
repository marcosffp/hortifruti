package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record SalesSummaryDetails(
    @JsonProperty("numero") String numero,
    @JsonProperty("mod") String mod,
    @JsonProperty("data") String data,
    @JsonProperty("envio") String envio,
    @JsonProperty("cliente") String cliente,
    @JsonProperty("subtotal") BigDecimal subtotal,
    @JsonProperty("desconto") BigDecimal desconto,
    @JsonProperty("acrescimo") BigDecimal acrescimo,
    @JsonProperty("total") BigDecimal total
) {}