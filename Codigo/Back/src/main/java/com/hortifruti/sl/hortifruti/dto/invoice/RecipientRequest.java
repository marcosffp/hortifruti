package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecipientRequest(
    @JsonProperty("cnpj") String cnpj,
    @JsonProperty("cpf") String cpf,
    @NotBlank(message = "Nome do destinatário é obrigatório") @JsonProperty("nome") String nome,
    @JsonProperty("nome_fantasia") String nomeFantasia,
    @JsonProperty("telefone") String telefone,
    @JsonProperty("email") String email,
    @NotNull(message = "Endereço do destinatário é obrigatório") @JsonProperty("endereco")
        AddressRequest endereco,
    @JsonProperty("inscricao_estadual") String inscricaoEstadual,
    @JsonProperty("indicador_inscricao_estadual") Integer indicadorInscricaoEstadual) {}
