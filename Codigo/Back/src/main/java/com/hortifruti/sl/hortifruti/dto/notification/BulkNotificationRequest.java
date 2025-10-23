package com.hortifruti.sl.hortifruti.dto.notification;

import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import java.math.BigDecimal;
import java.util.List;

public record BulkNotificationRequest(
    List<Long> clientIds,
    List<NotificationChannel> channels,
    String destinationType, // "clientes" ou "contabilidade"
    String customMessage,
    BigDecimal dueDate,
    BigDecimal billetValue) {}
