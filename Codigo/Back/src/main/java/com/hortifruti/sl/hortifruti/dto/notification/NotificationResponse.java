package com.hortifruti.sl.hortifruti.dto.notification;

public record NotificationResponse(
    boolean success, String message, String emailStatus, String whatsappStatus) {

  public NotificationResponse(boolean success, String message) {
    this(success, message, "N/A", "N/A");
  }

  public static NotificationResponse withStatuses(
      boolean success, String message, String emailStatus, String whatsappStatus) {
    return new NotificationResponse(success, message, emailStatus, whatsappStatus);
  }
}
