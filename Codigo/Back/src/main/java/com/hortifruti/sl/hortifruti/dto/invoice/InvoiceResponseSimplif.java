package com.hortifruti.sl.hortifruti.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InvoiceResponseSimplif(
    @JsonProperty("cnpj_destinatario")
    String cnpjDestinatario,
    
    @JsonProperty("valor_total")
    BigDecimal valorTotal,
    
    @JsonProperty("numero")
    String numero,
    
    @JsonProperty("status")
    String status,
    
    @JsonProperty("data_emissao")
    LocalDateTime dataEmissao,

    @JsonProperty("ref")
    String ref


) {}