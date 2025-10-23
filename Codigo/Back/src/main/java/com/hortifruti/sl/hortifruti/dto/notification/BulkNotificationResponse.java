package com.hortifruti.sl.hortifruti.dto.notification;

import java.util.List;

public record BulkNotificationResponse(
    boolean success,
    String message,
    int totalSent,
    int totalFailed,
    List<String> failedRecipients) {
  public static BulkNotificationResponse success(int totalSent, String message) {
    return new BulkNotificationResponse(true, message, totalSent, 0, List.of());
  }

  public static BulkNotificationResponse failure(String message, List<String> failedRecipients) {
    return new BulkNotificationResponse(
        false, message, 0, failedRecipients.size(), failedRecipients);
  }

  public static BulkNotificationResponse partial(
      int totalSent, int totalFailed, List<String> failedRecipients) {
    return new BulkNotificationResponse(
        true,
        String.format("Enviado para %d destinatário(s). %d falha(s).", totalSent, totalFailed),
        totalSent,
        totalFailed,
        failedRecipients);
  }
}
