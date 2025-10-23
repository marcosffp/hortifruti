package com.hortifruti.sl.hortifruti.dto.notification;

import java.math.BigDecimal;

public record GenericFilesAccountingRequest(
    BigDecimal debitValue, BigDecimal creditValue, BigDecimal cashValue, String customMessage) {}
