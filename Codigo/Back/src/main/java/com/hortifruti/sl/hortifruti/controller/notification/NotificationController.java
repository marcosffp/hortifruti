package com.hortifruti.sl.hortifruti.controller.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.notification.NotificationChannel;
import com.hortifruti.sl.hortifruti.service.notification.BulkNotificationService;
import com.hortifruti.sl.hortifruti.service.notification.NotificationService;
import com.hortifruti.sl.hortifruti.service.scheduler.DatabaseStorageAlertService;
import com.hortifruti.sl.hortifruti.service.scheduler.DatabaseStorageMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Notificações",
    description = "API para envio de notificações e documentos via email e WhatsApp")
public class NotificationController {

  private final NotificationService notificationService;
  private final BulkNotificationService bulkNotificationService;
  private final DatabaseStorageMonitorService databaseStorageMonitorService;
  private final DatabaseStorageAlertService databaseStorageAlertService;

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/accounting/recipients")
  @Operation(
      summary = "Listar destinatários da contabilidade",
      description =
          "Retorna os emails configurados em ACCOUNTING_EMAIL para receber envios da contabilidade")
  public ResponseEntity<List<String>> getAccountingRecipients() {
    return ResponseEntity.ok(notificationService.getAccountingRecipients());
  }

  @Operation(
      summary = "Enviar arquivos genéricos para contabilidade",
      description =
          "Upload de arquivos genéricos (opcional) e valores de cartão e dinheiro (opcionais). Envia apenas via email.")
  @PostMapping(value = "/accounting/generic-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NotificationResponse> sendGenericFilesToAccounting(
      @Parameter(
              description = "Arquivos a serem enviados para contabilidade (opcional)",
              content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
          @RequestParam(required = false)
          List<MultipartFile> files,
      @Parameter(description = "Valor do cartão (opcional)")
          @RequestParam(required = false, defaultValue = "0")
          String cardValue,
      @Parameter(description = "Valor em dinheiro (opcional)")
          @RequestParam(required = false, defaultValue = "0")
          String cashValue,
      @Parameter(description = "Mensagem personalizada (opcional)") @RequestParam(required = false)
          String customMessage) {
    GenericFilesAccountingRequest request =
        new GenericFilesAccountingRequest(
            new BigDecimal(cardValue), new BigDecimal(cashValue), customMessage);
    NotificationResponse response =
        notificationService.sendGenericFilesToAccounting(files, request);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Enviar documentos para cliente",
      description = "Envio de documentos para cliente específico via email e/ou WhatsApp")
  @PostMapping(value = "/client/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NotificationResponse> sendDocumentsToClient(
      @Parameter(
              description = "Arquivos a serem enviados para o cliente (opcional)",
              content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
          @RequestParam(required = false)
          List<MultipartFile> files,
      @Parameter(description = "ID do cliente") @RequestParam Long clientId,
      @Parameter(description = "Canal de comunicação (EMAIL, WHATSAPP, BOTH)") @RequestParam
          String channel,
      @Parameter(description = "Mensagem personalizada (opcional)") @RequestParam(required = false)
          String customMessage) {
    ClientDocumentsRequest request =
        new ClientDocumentsRequest(
            clientId, NotificationChannel.valueOf(channel.toUpperCase()), customMessage);
    NotificationResponse response = notificationService.sendDocumentsToClient(files, request);
    return ResponseEntity.ok(response);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping("/test/database-storage-alert")
  @Operation(
      summary = "Testar email de alerta de armazenamento, excluir depois",
      description = "Envia um email de teste com dados reais do banco de dados atual")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Email de teste enviado com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro ao enviar email de teste"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas administradores")
      })
  public ResponseEntity<Map<String, Object>> testDatabaseStorageAlert() {
    BigDecimal currentSizeMB = databaseStorageMonitorService.getDatabaseSizeInMB();
    databaseStorageAlertService.sendTestStorageNotification(currentSizeMB);

    BigDecimal maxSize = new BigDecimal("5120"); // 5GB
    BigDecimal storagePercentage =
        currentSizeMB
            .multiply(new BigDecimal("100"))
            .divide(maxSize, 1, java.math.RoundingMode.HALF_UP);

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "Email de teste de alerta de armazenamento enviado com sucesso");
    response.put("timestamp", LocalDateTime.now());
    response.put("currentStoragePercentage", storagePercentage + "%");
    response.put("currentSize", currentSizeMB + " MB");
    response.put("maxSize", maxSize + " MB");
    response.put("isOverThreshold", databaseStorageMonitorService.isDatabaseOverThreshold());

    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/send-bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Enviar notificações em massa",
      description = "Envia múltiplos arquivos para múltiplos clientes via e-mail e/ou WhatsApp")
  public ResponseEntity<BulkNotificationResponse> sendBulkNotifications(
      @RequestParam List<MultipartFile> files,
      @RequestParam List<Long> clientIds,
      @RequestParam List<String> channels,
      @RequestParam String destinationType,
      @RequestParam(required = false) String customMessage) {
    BulkNotificationResponse response =
        bulkNotificationService.sendBulkNotifications(
            files, clientIds, channels, destinationType, customMessage);
    return ResponseEntity.ok(response);
  }
}
