package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ItemTaxDetails(
    @JsonProperty("cfop") String cfop,
    @JsonProperty("valor_bruto") BigDecimal valorBruto,
    @JsonProperty("icms_situacao_tributaria") String icmsSituacaoTributaria
) {}
