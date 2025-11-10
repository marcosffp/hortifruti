package com.hortifruti.sl.hortifruti.dto.purchase.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClientResponse(
    Long id,
    String clientName,
    String email,
    String phoneNumber,
    String address,
    String document,
    boolean variablePrice,
    String stateRegistration,
    String stateIndicator,
    LocalDate lastPurchaseDate,
    BigDecimal totalPurchaseValue) {}
