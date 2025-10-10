package com.hortifruti.sl.hortifruti.controller.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import com.hortifruti.sl.hortifruti.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Notificações", description = "API para envio de notificações e documentos via email e WhatsApp")
public class NotificationController {

  private final NotificationService notificationService;

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
   * Envio para cliente - Boleto, Nota Fiscal, etc.
   */
  @Operation(summary = "Enviar documentos para cliente",
             description = "Envio de documentos para cliente com geração automática de boleto e nota fiscal se solicitado")
  @PostMapping(value = "/client/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<NotificationResponse> sendDocumentsToClient(
      @Parameter(description = "Arquivos opcionais a serem enviados para o cliente", 
                 content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
      @RequestParam(value = "files", required = false) List<MultipartFile> files,
      
      @Parameter(description = "ID do cliente")
      @RequestParam("clientId") Long clientId,
      @Parameter(description = "Canal de comunicação (EMAIL, WHATSAPP, BOTH)")
      @RequestParam("channel") String channel,
      @Parameter(description = "Gerar boleto automaticamente")
      @RequestParam(value = "generateBillet", defaultValue = "false") boolean generateBillet,
      @Parameter(description = "Gerar nota fiscal automaticamente")
      @RequestParam(value = "generateInvoice", defaultValue = "false") boolean generateInvoice,
      @Parameter(description = "Mensagem personalizada (opcional)")
      @RequestParam(value = "customMessage", required = false) String customMessage) {
    try {
      ClientDocumentsRequest request = new ClientDocumentsRequest(
          clientId,
          NotificationChannel.valueOf(channel.toUpperCase()),
          customMessage,
          generateBillet,
          generateInvoice
      );
      NotificationResponse response = notificationService.sendDocumentsToClient(files, request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(new NotificationResponse(false, "Erro ao enviar documentos para cliente: " + e.getMessage()));
    }
  }



  /**
   * Teste dos serviços de comunicação
   */
  @Operation(summary = "Testar serviços de comunicação",
             description = "Verifica se os serviços de email e WhatsApp estão configurados e funcionando corretamente")
  @PostMapping("/test-services")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<NotificationResponse> testServices() {
    try {
      NotificationResponse response = notificationService.testCommunicationServices();
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(new NotificationResponse(false, "Erro ao testar serviços: " + e.getMessage()));
    }
  }
}