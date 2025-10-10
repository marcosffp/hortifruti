package com.hortifruti.sl.hortifruti.dto.notification;

import com.hortifruti.sl.hortifruti.model.enumeration.NotificationType;
import java.util.List;

public record AccountingNotificationRequest(
    NotificationType notificationType,
    int month,
    int year,
    String customMessage,
    List<GenericFileRequest> additionalFiles,
    String debitValue,
    String creditValue,
    String cashValue) {}