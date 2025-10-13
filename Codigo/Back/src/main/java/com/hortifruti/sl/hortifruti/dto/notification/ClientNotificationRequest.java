package com.hortifruti.sl.hortifruti.dto.notification;

import com.hortifruti.sl.hortifruti.model.enumeration.NotificationType;
import java.util.List;

public record ClientNotificationRequest(
    Long clientId,
    NotificationType notificationType,
    List<GenericFileRequest> files,
    String customMessage,
    boolean includeBoleto,
    boolean includeNotaFiscal) {}