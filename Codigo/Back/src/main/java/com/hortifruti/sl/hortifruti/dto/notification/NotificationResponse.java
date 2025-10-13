package com.hortifruti.sl.hortifruti.dto.notification;

public record NotificationResponse(
    boolean success,
    String message,
    String emailStatus,
    String whatsappStatus) {
    
    // Construtor para respostas simples (apenas mensagem)
    public NotificationResponse(boolean success, String message) {
        this(success, message, "N/A", "N/A");
    }
    
    // Construtor para respostas com status específicos
    public static NotificationResponse withStatuses(boolean success, String message, String emailStatus, String whatsappStatus) {
        return new NotificationResponse(success, message, emailStatus, whatsappStatus);
    }
}