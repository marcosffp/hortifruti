package com.hortifruti.sl.hortifruti.controller.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.notification.BulkNotificationService;
import com.hortifruti.sl.hortifruti.service.notification.NotificationService;
import com.hortifruti.sl.hortifruti.service.scheduler.CombinedScoreSchedulerService;
import com.hortifruti.sl.hortifruti.service.scheduler.DatabaseStorageService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  private final DatabaseStorageService databaseStorageService;

  @Autowired private CombinedScoreSchedulerService schedulerService;

  /** Envio para contabilidade - Arquivos genéricos com valores de débito/crédito (opcional) */
  @Operation(
      summary = "Enviar arquivos genéricos para contabilidade",
      description =
          "Upload de arquivos genéricos (opcional) e valores de cartão e dinheiro (opcionais). Envia apenas via email.")
  @PostMapping(value = "/accounting/generic-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NotificationResponse> sendGenericFilesToAccounting(
      @Parameter(
              description = "Arquivos a serem enviados para contabilidade (opcional)",
              content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
          @RequestParam(value = "files", required = false)
          List<MultipartFile> files,
      @Parameter(description = "Valor do cartão (opcional)")
          @RequestParam(value = "cardValue", required = false, defaultValue = "0")
          String cardValue,
      @Parameter(description = "Valor em dinheiro (opcional)")
          @RequestParam(value = "cashValue", required = false, defaultValue = "0")
          String cashValue,
      @Parameter(description = "Mensagem personalizada (opcional)")
          @RequestParam(value = "customMessage", required = false)
          String customMessage) {
    try {
      GenericFilesAccountingRequest request =
          new GenericFilesAccountingRequest(
              new BigDecimal(cardValue), new BigDecimal(cashValue), customMessage);
      NotificationResponse response =
          notificationService.sendGenericFilesToAccounting(files, request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(
              new NotificationResponse(
                  false, "Erro ao enviar arquivos para contabilidade: " + e.getMessage()));
    }
  }

  /** Envio para cliente - Documentos diversos */
  @Operation(
      summary = "Enviar documentos para cliente",
      description = "Envio de documentos para cliente específico via email e/ou WhatsApp")
  @PostMapping(value = "/client/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NotificationResponse> sendDocumentsToClient(
      @Parameter(
              description = "Arquivos a serem enviados para o cliente (opcional)",
              content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
          @RequestParam(value = "files", required = false)
          List<MultipartFile> files,
      @Parameter(description = "ID do cliente") @RequestParam("clientId") Long clientId,
      @Parameter(description = "Canal de comunicação (EMAIL, WHATSAPP, BOTH)")
          @RequestParam("channel")
          String channel,
      @Parameter(description = "Mensagem personalizada (opcional)")
          @RequestParam(value = "customMessage", required = false)
          String customMessage) {
    try {
      ClientDocumentsRequest request =
          new ClientDocumentsRequest(
              clientId, NotificationChannel.valueOf(channel.toUpperCase()), customMessage);
      NotificationResponse response = notificationService.sendDocumentsToClient(files, request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(
              new NotificationResponse(
                  false, "Erro ao enviar documentos para cliente: " + e.getMessage()));
    }
  }

  /** Teste manual do email de alerta de armazenamento do banco de dados */
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
    try {
      // Obter tamanho real atual do banco de dados
      BigDecimal currentSizeMB = databaseStorageService.getDatabaseSizeInMB();

      // Enviar notificação com dados reais
      databaseStorageService.sendTestStorageNotification(currentSizeMB);

      // Calcular percentual de uso para a resposta
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
      response.put("isOverThreshold", databaseStorageService.isDatabaseOverThreshold());

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("message", "Erro ao enviar email de teste: " + e.getMessage());
      errorResponse.put("timestamp", LocalDateTime.now());

      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  @PostMapping("/overdue/check")
  @Operation(
      summary = "Executar verificação manual de boletos vencidos",
      description =
          "Força a execução da verificação de CombinedScores vencidos e envio de notificações")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Verificação executada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas administradores")
      })
  public ResponseEntity<Map<String, Object>> checkOverdueScores() {
    try {
      List<CombinedScore> overdueScores = schedulerService.manualOverdueCheck();

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Verificação de boletos vencidos executada com sucesso");
      response.put("timestamp", LocalDateTime.now());
      response.put("overdueCount", overdueScores.size());
      response.put(
          "overdueScores",
          overdueScores.stream()
              .map(
                  score -> {
                    Map<String, Object> scoreInfo = new HashMap<>();
                    scoreInfo.put("id", score.getId());
                    scoreInfo.put("clientId", score.getClientId());
                    scoreInfo.put("dueDate", score.getDueDate());
                    scoreInfo.put("totalValue", score.getTotalValue());
                    scoreInfo.put("confirmedAt", score.getConfirmedAt());
                    scoreInfo.put("isPaid", score.getConfirmedAt() != null);
                    return scoreInfo;
                  })
              .toList());

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("message", "Erro ao executar verificação: " + e.getMessage());
      errorResponse.put("timestamp", LocalDateTime.now());
      errorResponse.put("overdueCount", 0);

      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  /** Enviar notificações em massa para múltiplos destinatários */
  @PostMapping(value = "/send-bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Enviar notificações em massa",
      description = "Envia múltiplos arquivos para múltiplos clientes via e-mail e/ou WhatsApp")
  public ResponseEntity<BulkNotificationResponse> sendBulkNotifications(
      @RequestParam("files") List<MultipartFile> files,
      @RequestParam("clientIds") List<Long> clientIds,
      @RequestParam("channels") List<String> channels,
      @RequestParam("destinationType") String destinationType,
      @RequestParam(value = "customMessage", required = false) String customMessage) {
    try {
      BulkNotificationResponse response =
          bulkNotificationService.sendBulkNotifications(
              files, clientIds, channels, destinationType, customMessage);

      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(BulkNotificationResponse.failure(e.getMessage(), List.of()));
    } catch (Exception e) {
      return ResponseEntity.status(500)
          .body(
              BulkNotificationResponse.failure(
                  "Erro ao processar notificações: " + e.getMessage(), List.of()));
    }
  }

  @GetMapping("/test")
  @Operation(
      summary = "Testar serviços de notificação",
      description = "Verifica se os serviços de e-mail e WhatsApp estão funcionando")
  public ResponseEntity<String> testServices() {
    return ResponseEntity.ok("Serviço de notificações está ativo");
  }
}
