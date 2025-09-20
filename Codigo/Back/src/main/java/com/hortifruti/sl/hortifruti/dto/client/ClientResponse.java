package com.hortifruti.sl.hortifruti.dto.client;

public record ClientResponse(
    Long id,
    String clientName,
    String email,
    String phoneNumber,
    String address,
    String document,
    boolean variablePrice) {}
