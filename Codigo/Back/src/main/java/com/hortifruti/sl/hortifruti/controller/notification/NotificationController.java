package com.hortifruti.sl.hortifruti.controller.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.CombinedScore;
import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import com.hortifruti.sl.hortifruti.service.CombinedScoreSchedulerService;
import com.hortifruti.sl.hortifruti.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Notificações", description = "API para envio de notificações e documentos via email e WhatsApp")
public class NotificationController {

  private final NotificationService notificationService;

  @Autowired
  private CombinedScoreSchedulerService schedulerService;


  /**
   * Envio para contabilidade - Notas fiscais do mês anterior + extratos bancários
   */
  @Operation(summary = "Enviar extratos mensais para contabilidade",
             description = "Envia ZIP com notas fiscais, extratos bancários (BB e Sicoob) e planilhas Excel para contabilidade")
  @PostMapping("/accounting/monthly-statements")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<NotificationResponse> sendMonthlyStatements(
      @Parameter(description = "Dados da requisição com mês, ano e canal de comunicação")
      @RequestBody MonthlyStatementsRequest request) {
    try {
      NotificationResponse response = notificationService.sendMonthlyStatements(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(new NotificationResponse(false, "Erro ao enviar extratos mensais: " + e.getMessage()));
    }
  }

  /**
   * Envio para contabilidade - Arquivos genéricos com valores de débito/crédito
   */
  @Operation(summary = "Enviar arquivos genéricos para contabilidade",
             description = "Upload de arquivos genéricos com cálculo automático de redução de 60% nos valores de débito e crédito")
  @PostMapping(value = "/accounting/generic-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<NotificationResponse> sendGenericFilesToAccounting(
      @Parameter(description = "Arquivos a serem enviados para contabilidade", 
                 content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
      @RequestParam("files") List<MultipartFile> files,
      
      @Parameter(description = "Canal de comunicação (EMAIL, WHATSAPP, BOTH)")
      @RequestParam("channel") String channel,
      @Parameter(description = "Valor de débito")
      @RequestParam("debitValue") String debitValue,
      @Parameter(description = "Valor de crédito")
      @RequestParam("creditValue") String creditValue,
      @Parameter(description = "Valor em dinheiro (opcional)")
      @RequestParam(value = "cashValue", required = false, defaultValue = "0") String cashValue,
      @Parameter(description = "Mensagem personalizada (opcional)")
      @RequestParam(value = "customMessage", required = false) String customMessage) {
    try {
      GenericFilesAccountingRequest request = new GenericFilesAccountingRequest(
          NotificationChannel.valueOf(channel.toUpperCase()),
          new BigDecimal(debitValue),
          new BigDecimal(creditValue),
          new BigDecimal(cashValue),
          customMessage
      );
      NotificationResponse response = notificationService.sendGenericFilesToAccounting(files, request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(new NotificationResponse(false, "Erro ao enviar arquivos para contabilidade: " + e.getMessage()));
    }
  }

  /**
   * Envio para cliente - Documentos diversos
   */
  @Operation(summary = "Enviar documentos para cliente",
             description = "Envio de documentos para cliente específico via email e/ou WhatsApp")
  @PostMapping(value = "/client/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<NotificationResponse> sendDocumentsToClient(
      @Parameter(description = "Arquivos a serem enviados para o cliente", 
                 content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
      @RequestParam("files") List<MultipartFile> files,
      
      @Parameter(description = "ID do cliente")
      @RequestParam("clientId") Long clientId,
      @Parameter(description = "Canal de comunicação (EMAIL, WHATSAPP, BOTH)")
      @RequestParam("channel") String channel,
      @Parameter(description = "Mensagem personalizada (opcional)")
      @RequestParam(value = "customMessage", required = false) String customMessage) {
    try {
      ClientDocumentsRequest request = new ClientDocumentsRequest(
          clientId,
          NotificationChannel.valueOf(channel.toUpperCase()),
          customMessage
      );
      NotificationResponse response = notificationService.sendDocumentsToClient(files, request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(new NotificationResponse(false, "Erro ao enviar documentos para cliente: " + e.getMessage()));
    }
  }


    @PostMapping("/overdue/check")
    @Operation(summary = "Executar verificação manual de boletos vencidos", 
               description = "Força a execução da verificação de CombinedScores vencidos e envio de notificações")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificação executada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas administradores")
    })
    public ResponseEntity<Map<String, Object>> checkOverdueScores() {
        try {
            log.info("Solicitação de verificação manual de boletos vencidos recebida");
            
            List<CombinedScore> overdueScores = schedulerService.manualOverdueCheck();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verificação de boletos vencidos executada com sucesso");
            response.put("timestamp", LocalDateTime.now());
            response.put("overdueCount", overdueScores.size());
            response.put("overdueScores", overdueScores.stream().map(score -> {
                Map<String, Object> scoreInfo = new HashMap<>();
                scoreInfo.put("id", score.getId());
                scoreInfo.put("clientId", score.getClientId());
                scoreInfo.put("dueDate", score.getDueDate());
                scoreInfo.put("totalValue", score.getTotalValue());
                scoreInfo.put("confirmedAt", score.getConfirmedAt());
                scoreInfo.put("isPaid", score.getConfirmedAt() != null);
                return scoreInfo;
            }).toList());
            
            log.info("Verificação manual concluída. Encontrados {} boletos vencidos", overdueScores.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao executar verificação manual de boletos vencidos", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao executar verificação: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            errorResponse.put("overdueCount", 0);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}