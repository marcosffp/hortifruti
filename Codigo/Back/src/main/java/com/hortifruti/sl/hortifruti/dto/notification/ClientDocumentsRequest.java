package com.hortifruti.sl.hortifruti.dto.notification;

import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;

public record ClientDocumentsRequest(
    Long clientId,
    NotificationChannel channel,
    String customMessage
) {}