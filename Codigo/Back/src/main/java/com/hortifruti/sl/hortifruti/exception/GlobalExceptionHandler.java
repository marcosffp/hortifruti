package com.hortifruti.sl.hortifruti.exception;

import com.hortifruti.sl.hortifruti.exception.auth.AccountLockedException;
import com.hortifruti.sl.hortifruti.exception.auth.AuthException;
import com.hortifruti.sl.hortifruti.exception.auth.TokenException;
import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.exception.bb.BBApiException;
import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import com.hortifruti.sl.hortifruti.exception.climate.ProductException;
import com.hortifruti.sl.hortifruti.exception.climate.RecommendationException;
import com.hortifruti.sl.hortifruti.exception.finance.TransactionException;
import com.hortifruti.sl.hortifruti.exception.freight.DistanceException;
import com.hortifruti.sl.hortifruti.exception.freight.FreightException;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.exception.notification.NotificationException;
import com.hortifruti.sl.hortifruti.exception.purchase.ClientException;
import com.hortifruti.sl.hortifruti.exception.purchase.CombinedScoreException;
import com.hortifruti.sl.hortifruti.exception.purchase.PurchaseException;
import com.hortifruti.sl.hortifruti.exception.sicoob.SicoobExtratoException;
import com.hortifruti.sl.hortifruti.exception.storage.StorageException;
import com.hortifruti.sl.hortifruti.exception.user.UserException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Alguns endpoints de download (ex.: /transactions/export-complete) declaram {@code produces =
   * "application/zip"} no mapeamento. Sem fixar o Content-Type aqui, o Spring tenta reusar esse
   * media type "producible" ao serializar o corpo de erro (um {@link Map}), e como não existe
   * conversor de Map para application/zip, a resposta de erro falha por sua vez (HttpMessageNotWritableException:
   * "No converter for [class java.util.HashMap] with preset Content-Type"), escondendo a mensagem
   * real do erro e devolvendo um 500 vazio ao cliente.
   */
  private ResponseEntity<Map<String, String>> errorResponse(
      HttpStatus status, String error, String message) {
    Map<String, String> body = new HashMap<>();
    body.put("error", error);
    body.put("message", message);
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
  }

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<Map<String, String>> handleAuthException(
      AuthException ex, HttpServletRequest request) {
    log.warn("Erro de autenticação em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.UNAUTHORIZED, "Erro de Autenticação", ex.getMessage());
  }

  /**
   * Retorna o mesmo status/mensagem genérica de {@link AuthException} (indistinguível de senha
   * errada ou usuário inexistente, para evitar enumeration attack), acrescentando apenas
   * {@code retryAfter} em segundos — usado pelo front para saber quando reabilitar o botão de
   * login, sem expor "conta bloqueada" como texto visível.
   */
  @ExceptionHandler(AccountLockedException.class)
  public ResponseEntity<Map<String, Object>> handleAccountLockedException(
      AccountLockedException ex, HttpServletRequest request) {
    log.warn(
        "Tentativa de login bloqueada em {}: retryAfter={}s",
        request.getRequestURI(),
        ex.getRetryAfterSeconds());
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Erro de Autenticação");
    body.put("message", ex.getMessage());
    body.put("retryAfter", ex.getRetryAfterSeconds());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  @ExceptionHandler(TokenException.class)
  public ResponseEntity<Map<String, String>> handleTokenException(
      TokenException ex, HttpServletRequest request) {
    log.warn("Erro de token em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.FORBIDDEN, "Erro de Token", ex.getMessage());
  }

  /**
   * {@code @PreAuthorize} nega acesso dentro da invocação do método do controller (via AOP), não
   * na cadeia de filtros do Spring Security — então essa exceção nunca chega até o
   * ExceptionTranslationFilter (que normalmente a converteria em 403). Sem este handler específico,
   * ela caía no catch-all genérico e virava um 500 confuso para qualquer usuário sem o papel exigido.
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> handleAccessDeniedException(
      AccessDeniedException ex, HttpServletRequest request) {
    log.warn("Acesso negado em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(
        HttpStatus.FORBIDDEN,
        "Acesso Negado",
        "Você não tem permissão para acessar este recurso.");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message;
    if (!ex.getBindingResult().getFieldErrors().isEmpty()) {
      message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
    } else {
      message = "Dados fornecidos são inválidos. Por favor, verifique e tente novamente.";
    }

    log.warn("Erro de validação em {}: {}", request.getRequestURI(), message);
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de validação", message);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Erro de integridade de dados em {}: {}", request.getRequestURI(), ex.getMessage());

    String errorMessage = ex.getMessage();
    if (errorMessage != null && errorMessage.contains("Duplicate entry")) {
      if (errorMessage.contains("users")) {
        return errorResponse(
            HttpStatus.CONFLICT,
            "Erro de validação",
            "Nome de usuário já está em uso. Por favor, escolha outro nome de usuário.");
      }
      return errorResponse(
          HttpStatus.CONFLICT,
          "Erro de validação",
          "Registro duplicado detectado. Por favor, verifique os dados fornecidos.");
    }

    return errorResponse(
        HttpStatus.BAD_REQUEST,
        "Erro de validação",
        "Erro de integridade dos dados. Por favor, verifique as informações fornecidas.");
  }

  @ExceptionHandler(TransactionException.class)
  public ResponseEntity<Map<String, String>> handleTransactionException(
      TransactionException ex, HttpServletRequest request) {
    log.warn("Erro de transação em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de Transação", ex.getMessage());
  }

  @ExceptionHandler(ClientException.class)
  public ResponseEntity<Map<String, String>> handleClientException(
      ClientException ex, HttpServletRequest request) {
    log.warn("Erro de cliente em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de Cliente", ex.getMessage());
  }

  @ExceptionHandler(UserException.class)
  public ResponseEntity<Map<String, String>> handleUserException(
      UserException ex, HttpServletRequest request) {
    log.warn("Erro de usuário em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de Usuário", ex.getMessage());
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<Map<String, String>> handleHttpMediaTypeNotSupportedException(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    log.warn(
        "Tipo de conteúdo não suportado em {}: {}", request.getRequestURI(), ex.getContentType());
    return errorResponse(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "Tipo de conteúdo não suportado",
        "O tipo de conteúdo '"
            + ex.getContentType()
            + "' não é suportado para este endpoint. Use 'multipart/form-data' para upload de arquivos.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGenericException(
      Exception ex, HttpServletRequest request) {
    log.error("Erro interno não tratado em {}", request.getRequestURI(), ex);
    return errorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Erro interno do servidor",
        "Ocorreu um erro ao processar sua solicitação. Por favor, tente novamente mais tarde.");
  }

  @ExceptionHandler(org.springframework.dao.DataAccessResourceFailureException.class)
  public ResponseEntity<Map<String, String>> handleDatabaseConnectionException(
      org.springframework.dao.DataAccessResourceFailureException ex, HttpServletRequest request) {
    log.error("Erro de conexão com o banco de dados em {}", request.getRequestURI(), ex);
    return errorResponse(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Erro de Conexão com o Banco de Dados",
        "Não foi possível conectar ao banco de dados. Por favor, tente novamente mais tarde.");
  }

  @ExceptionHandler(FreightException.class)
  public ResponseEntity<Map<String, String>> handleFreightException(
      FreightException ex, HttpServletRequest request) {
    log.warn("Erro de frete em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de Frete", ex.getMessage());
  }

  @ExceptionHandler(DistanceException.class)
  public ResponseEntity<Map<String, String>> handleDistanceException(
      DistanceException ex, HttpServletRequest request) {
    log.warn("Erro de distância em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de Distância", ex.getMessage());
  }

  @ExceptionHandler(BilletException.class)
  public ResponseEntity<Map<String, String>> handleBilletException(
      BilletException ex, HttpServletRequest request) {
    log.error("Erro na integração com Sicoob em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro na Integração com Sicoob", ex.getMessage());
  }

  @ExceptionHandler(BBApiException.class)
  public ResponseEntity<Map<String, String>> handleBBApiException(
      BBApiException ex, HttpServletRequest request) {
    log.error(
        "Erro na integração com o Banco do Brasil em {}: {}",
        request.getRequestURI(),
        ex.getMessage());
    return errorResponse(
        HttpStatus.BAD_GATEWAY, "Erro na Integração com o Banco do Brasil", ex.getMessage());
  }

  @ExceptionHandler(SicoobExtratoException.class)
  public ResponseEntity<Map<String, String>> handleSicoobExtratoException(
      SicoobExtratoException ex, HttpServletRequest request) {
    log.error(
        "Erro ao consultar extrato do Sicoob em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(
        HttpStatus.BAD_GATEWAY, "Erro ao consultar extrato do Sicoob", ex.getMessage());
  }

  @ExceptionHandler(PurchaseException.class)
  public ResponseEntity<Map<String, String>> handlePurchaseException(
      PurchaseException ex, HttpServletRequest request) {
    log.warn("Erro no processamento da compra em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(
        HttpStatus.BAD_REQUEST, "Erro no Processamento da Compra", ex.getMessage());
  }

  @ExceptionHandler(ProductException.class)
  public ResponseEntity<Map<String, String>> handleProductException(
      ProductException ex, HttpServletRequest request) {
    log.warn("Erro de produto em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de Produto", ex.getMessage());
  }

  @ExceptionHandler(CombinedScoreException.class)
  public ResponseEntity<Map<String, String>> handleCombinedScoreException(
      CombinedScoreException ex, HttpServletRequest request) {
    log.warn(
        "Erro no agrupamento de pontuação combinada em {}: {}",
        request.getRequestURI(),
        ex.getMessage());
    return errorResponse(
        HttpStatus.BAD_REQUEST, "Erro no Agrupamento de Pontuação Combinada", ex.getMessage());
  }

  @ExceptionHandler(RecommendationException.class)
  public ResponseEntity<Map<String, String>> handleRecommendationException(
      RecommendationException ex, HttpServletRequest request) {
    log.warn("Erro de recomendação em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de Recomendação", ex.getMessage());
  }

  @ExceptionHandler(BackupException.class)
  public ResponseEntity<Map<String, String>> handleBackupException(
      BackupException ex, HttpServletRequest request) {
    log.error("Erro de backup em {}", request.getRequestURI(), ex);
    return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de Backup", ex.getMessage());
  }

  @ExceptionHandler(NotificationException.class)
  public ResponseEntity<Map<String, String>> handleBulkNotificationException(
      NotificationException ex, HttpServletRequest request) {
    log.warn("Erro de notificação em massa em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(
        HttpStatus.BAD_REQUEST, "Erro de Notificação em Massa", ex.getMessage());
  }

  @ExceptionHandler(InvoiceException.class)
  public ResponseEntity<Map<String, String>> handleInvoiceException(
      InvoiceException ex, HttpServletRequest request) {
    log.warn("Erro de fatura em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de Fatura", ex.getMessage());
  }

  @ExceptionHandler(StorageException.class)
  public ResponseEntity<Map<String, String>> handleStorageException(
      StorageException ex, HttpServletRequest request) {
    log.error("Erro de armazenamento em {}", request.getRequestURI(), ex);
    return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de Armazenamento", ex.getMessage());
  }
}
