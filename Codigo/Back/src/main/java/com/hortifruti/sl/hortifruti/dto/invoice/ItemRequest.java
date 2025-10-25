package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ItemRequest(
    @NotBlank(message = "Código do produto é obrigatório")
    @JsonProperty("codigo_produto")
    String codigoProduto,

    @NotBlank(message = "Descrição do produto é obrigatória")
    @JsonProperty("descricao")
    String descricao,

    @NotBlank(message = "NCM é obrigatório")
    @JsonProperty("ncm")
    String ncm,

    @NotBlank(message = "CFOP é obrigatório")
    @JsonProperty("cfop")
    String cfop,

    @NotBlank(message = "Unidade comercial é obrigatória")
    @JsonProperty("unidade_comercial")
    String unidadeComercial,

    @NotNull(message = "Quantidade comercial é obrigatória")
    @Positive(message = "Quantidade comercial deve ser positiva")
    @JsonProperty("quantidade_comercial")
    BigDecimal quantidadeComercial,

    @NotNull(message = "Valor unitário é obrigatório")
    @Positive(message = "Valor unitário deve ser positivo")
    @JsonProperty("valor_unitario_comercial")
    BigDecimal valorUnitarioComercial,

    @NotNull(message = "Valor bruto é obrigatório")
    @PositiveOrZero(message = "Valor bruto deve ser positivo ou zero")
    @JsonProperty("valor_bruto")
    BigDecimal valorBruto,

    @JsonProperty("unidade_tributavel")
    String unidadeTributavel,

    @JsonProperty("quantidade_tributavel")
    BigDecimal quantidadeTributavel,

    @JsonProperty("valor_unitario_tributavel")
    BigDecimal valorUnitarioTributavel,

    @JsonProperty("icms_situacao_tributaria")
    String icmsSituacaoTributaria,

    @JsonProperty("icms_origem")
    String icmsOrigem,

    @JsonProperty("pis_situacao_tributaria")
    String pisSituacaoTributaria,

    @JsonProperty("cofins_situacao_tributaria")
    String cofinsSituacaoTributaria
) {
  
}