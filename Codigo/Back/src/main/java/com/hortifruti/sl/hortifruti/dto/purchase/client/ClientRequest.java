package com.hortifruti.sl.hortifruti.dto.purchase.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientRequest(
    @NotBlank(message = "Nome do cliente é obrigatório") String clientName,
    String nickname,
    @NotNull(message = "Preço variável é obrigatório") Boolean variablePrice,
    @Email(message = "Email deve ser válido") String email,
    String phoneNumber,
    @NotBlank(message = "Documento é obrigatório") String document,
    @NotBlank(message = "Endereço é obrigatório") String address,
    String stateRegistration,
    Integer stateIndicator,
    String cideCode,
    boolean onlyBillet,
    boolean requiresPurchaseProof) {}
