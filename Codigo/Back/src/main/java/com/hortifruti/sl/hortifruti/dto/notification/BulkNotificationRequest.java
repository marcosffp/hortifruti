package com.hortifruti.sl.hortifruti.dto.notification;

import com.hortifruti.sl.hortifruti.model.notification.NotificationChannel;
import java.math.BigDecimal;
import java.util.List;

public record BulkNotificationRequest(
    List<Long> clientIds,
    List<NotificationChannel> channels,
    String destinationType,
    String customMessage,
    BigDecimal dueDate,
    BigDecimal billetValue) {}
