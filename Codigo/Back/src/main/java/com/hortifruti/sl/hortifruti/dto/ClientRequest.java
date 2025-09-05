package com.hortifruti.sl.hortifruti.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
    @NotBlank(message = "Nome do cliente é obrigatório") String clientName,
    @Email(message = "Email deve ser válido") String email,
    @NotBlank(message = "Telefone é obrigatório") String phoneNumber,
    @NotBlank(message = "Endereço é obrigatório") String address) {}
