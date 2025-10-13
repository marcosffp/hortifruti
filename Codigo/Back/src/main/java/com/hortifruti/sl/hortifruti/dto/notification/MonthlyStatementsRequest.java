package com.hortifruti.sl.hortifruti.dto.notification;

import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;

public record MonthlyStatementsRequest(
    int month,
    int year,
    NotificationChannel channel, // EMAIL, WHATSAPP, BOTH
    String customMessage
) {}