package com.hortifruti.sl.hortifruti.dto.notification;

public record NotificationResponse(
    boolean success,
    String message,
    String emailStatus,
    String whatsappStatus) {}