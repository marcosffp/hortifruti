package com.hortifruti.sl.hortifruti.dto.notification;

import java.math.BigDecimal;

public record GenericFilesAccountingRequest(
    BigDecimal cardValue, BigDecimal cashValue, String customMessage) {}
