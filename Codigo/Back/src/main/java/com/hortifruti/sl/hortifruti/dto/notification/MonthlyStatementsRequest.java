package com.hortifruti.sl.hortifruti.dto.notification;

import com.hortifruti.sl.hortifruti.model.notification.NotificationChannel;

public record MonthlyStatementsRequest(
    int month, int year, NotificationChannel channel, String customMessage) {}
