package com.hortifruti.sl.hortifruti.dto;

public record ClientResponse(
    Long id,
    String clientName,
    String email,
    String phoneNumber,
    String address,
    boolean variablePrice) {}
