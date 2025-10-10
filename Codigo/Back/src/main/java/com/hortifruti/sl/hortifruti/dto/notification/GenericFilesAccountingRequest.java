package com.hortifruti.sl.hortifruti.dto.notification;

import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import java.math.BigDecimal;

public record GenericFilesAccountingRequest(
    NotificationChannel channel,
    BigDecimal debitValue,
    BigDecimal creditValue,
    BigDecimal cashValue,
    String customMessage
) {}