package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Logradouro é obrigatório")
        @JsonProperty("logradouro")
        String logradouro,

        @NotBlank(message = "Número é obrigatório")
        @JsonProperty("numero")
        String numero,

        @JsonProperty("complemento")
        String complemento,

        @NotBlank(message = "Bairro é obrigatório")
        @JsonProperty("bairro")
        String bairro,

        @NotBlank(message = "Município é obrigatório")
        @JsonProperty("municipio")
        String municipio,

        @NotBlank(message = "UF é obrigatória")
        @JsonProperty("uf")
        @Size(min = 2, max = 2)
        String uf,

        @NotBlank(message = "CEP é obrigatório")
        @JsonProperty("cep")
        String cep,

        @JsonProperty("codigo_municipio")
        String codigoMunicipio,

        @JsonProperty("codigo_pais")
        String codigoPais,

        @JsonProperty("nome_pais")
        String nomePais
    ) {}
