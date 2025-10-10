package com.hortifruti.sl.hortifruti.controller.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @PostMapping("/accounting")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<NotificationResponse> sendAccountingNotification(
      @RequestBody AccountingNotificationRequest request) {
    try {
      NotificationResponse response = notificationService.sendAccountingNotification(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(new NotificationResponse(false, "Erro ao enviar notificação contábil: " + e.getMessage(), "ERRO", "ERRO"));
    }
  }

  @PostMapping("/client")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<NotificationResponse> sendClientNotification(
      @RequestBody ClientNotificationRequest request) {
    try {
      NotificationResponse response = notificationService.sendClientNotification(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(new NotificationResponse(false, "Erro ao enviar notificação para cliente: " + e.getMessage(), "ERRO", "ERRO"));
    }
  }

  @PostMapping("/overdue-check")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<NotificationResponse> checkOverdueBillets() {
    try {
      NotificationResponse response = notificationService.checkAndNotifyOverdueBillets();
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(new NotificationResponse(false, "Erro ao verificar boletos vencidos: " + e.getMessage(), "N/A", "N/A"));
    }
  }

  @PostMapping("/test-services")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<NotificationResponse> testServices() {
    try {
      // Testa conectividade com WhatsApp e Email
      boolean emailOk = notificationService.testEmailService();
      boolean whatsappOk = notificationService.testWhatsAppService();
      
      String emailStatus = emailOk ? "OK" : "FALHA";
      String whatsappStatus = whatsappOk ? "OK" : "FALHA";
      String message = String.format("Teste de serviços - Email: %s, WhatsApp: %s", emailStatus, whatsappStatus);
      
      return ResponseEntity.ok(new NotificationResponse(emailOk && whatsappOk, message, emailStatus, whatsappStatus));
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(new NotificationResponse(false, "Erro ao testar serviços: " + e.getMessage(), "ERRO", "ERRO"));
    }
  }
}