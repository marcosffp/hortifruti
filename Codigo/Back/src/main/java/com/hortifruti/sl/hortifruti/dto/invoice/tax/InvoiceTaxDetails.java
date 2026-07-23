package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceTaxDetails(
    @JsonProperty("status") String status,
    @JsonProperty("numero") String numero,
    @JsonProperty("data_emissao") LocalDateTime dataEmissao,
    @JsonProperty("valor_produtos") BigDecimal valorProdutos,
    @JsonProperty("valor_total") BigDecimal valorTotal,
    @JsonProperty("icms_base_calculo") BigDecimal icmsBaseCalculo,
    @JsonProperty("icms_valor_total") BigDecimal icmsValorTotal,
    @JsonProperty("tributables") List<ItemTaxDetails> tributables,
    @JsonProperty("ref") String ref) {}
