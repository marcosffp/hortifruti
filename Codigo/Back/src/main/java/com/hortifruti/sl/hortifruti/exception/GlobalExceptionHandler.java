package com.hortifruti.sl.hortifruti.exception;

import com.hortifruti.sl.hortifruti.exception.auth.AccountLockedException;
import com.hortifruti.sl.hortifruti.exception.climate.WeatherApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLIntegrityConstraintViolationException;
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
   * conversor de Map para application/zip, a resposta de erro falha por sua vez
   * (HttpMessageNotWritableException: "No converter for [class java.util.HashMap] with preset
   * Content-Type"), escondendo a mensagem real do erro e devolvendo um 500 vazio ao cliente.
   */
  private ResponseEntity<Map<String, String>> errorResponse(
      HttpStatus status, String error, String message) {
    Map<String, String> body = new HashMap<>();
    body.put("error", error);
    body.put("message", message);
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
  }

  /**
   * Handler único para toda exceção que estende {@link DomainException} — cobre o que antes eram
   * ~25 handlers quase idênticos (um por tipo de exceção de domínio), cada um só variando
   * status/título/nível de log. Exceções com lógica própria (extrair campo de validação, decidir
   * por causa/conteúdo, expor campo extra) continuam com handler dedicado abaixo, fora deste fluxo
   * genérico.
   */
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<Map<String, String>> handleDomainException(
      DomainException ex, HttpServletRequest request) {
    if (ex.logStackTrace()) {
      log.error("{} em {}", ex.getErrorTitle(), request.getRequestURI(), ex);
    } else if (ex.isSevere()) {
      log.error("{} em {}: {}", ex.getErrorTitle(), request.getRequestURI(), ex.getMessage());
    } else {
      log.warn("{} em {}: {}", ex.getErrorTitle(), request.getRequestURI(), ex.getMessage());
    }
    return errorResponse(ex.getHttpStatus(), ex.getErrorTitle(), ex.getMessage());
  }

  /**
   * {@link AccountLockedException} estende {@code AuthException} (logo também é uma {@link
   * DomainException}), mas precisa de handler próprio porque acrescenta {@code retryAfter} ao
   * corpo — o resolvedor de {@code @ExceptionHandler} do Spring escolhe o tipo mais específico
   * registrado, então este handler tem prioridade sobre o genérico acima para essa subclasse.
   * Retorna o mesmo status/mensagem genérica de {@code AuthException} (indistinguível de senha
   * errada ou usuário inexistente, para evitar enumeration attack), acrescentando apenas {@code
   * retryAfter} em segundos — usado pelo front para saber quando reabilitar o botão de login, sem
   * expor "conta bloqueada" como texto visível.
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

  /**
   * {@code @PreAuthorize} nega acesso dentro da invocação do método do controller (via AOP), não na
   * cadeia de filtros do Spring Security — então essa exceção nunca chega até o
   * ExceptionTranslationFilter (que normalmente a converteria em 403). Sem este handler específico,
   * ela caía no catch-all genérico e virava um 500 confuso para qualquer usuário sem o papel
   * exigido.
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> handleAccessDeniedException(
      AccessDeniedException ex, HttpServletRequest request) {
    log.warn("Acesso negado em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(
        HttpStatus.FORBIDDEN, "Acesso Negado", "Você não tem permissão para acessar este recurso.");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn("Argumento inválido em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, "Erro de validação", ex.getMessage());
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

  /**
   * Identifica a causa por tipo ({@link SQLIntegrityConstraintViolationException}) e pelo nome
   * explícito da constraint (ex.: {@code uk_users_username}, ver {@code User}), em vez de checar
   * substring de {@code ex.getMessage()} — que depende do texto exato devolvido pelo driver JDBC
   * (varia por versão/locale do MySQL) e quebraria silenciosamente numa troca de driver.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Erro de integridade de dados em {}: {}", request.getRequestURI(), ex.getMessage());

    SQLIntegrityConstraintViolationException constraintViolation =
        findConstraintViolationCause(ex);
    if (constraintViolation != null) {
      String constraintMessage = constraintViolation.getMessage();
      if (constraintMessage != null && constraintMessage.contains("uk_users_username")) {
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

  private SQLIntegrityConstraintViolationException findConstraintViolationCause(Throwable ex) {
    Throwable cause = ex;
    while (cause != null) {
      if (cause instanceof SQLIntegrityConstraintViolationException violation) {
        return violation;
      }
      cause = cause.getCause();
    }
    return null;
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

  @ExceptionHandler(org.springframework.dao.DataAccessResourceFailureException.class)
  public ResponseEntity<Map<String, String>> handleDatabaseConnectionException(
      org.springframework.dao.DataAccessResourceFailureException ex, HttpServletRequest request) {
    log.error("Erro de conexão com o banco de dados em {}", request.getRequestURI(), ex);
    return errorResponse(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Erro de Conexão com o Banco de Dados",
        "Não foi possível conectar ao banco de dados. Por favor, tente novamente mais tarde.");
  }

  /**
   * A OpenWeather API está indisponível/instável — 503, não 500: o problema é do serviço externo,
   * não da nossa aplicação (contrato já documentado em {@code
   * WeatherForecastController#getFiveDayForecast}). {@link WeatherApiException} fica fora da
   * hierarquia de {@link DomainException} de propósito: é checked ({@code extends Exception}), não
   * {@code RuntimeException} — ver {@code exception/climate/README.md}.
   */
  @ExceptionHandler(WeatherApiException.class)
  public ResponseEntity<Map<String, String>> handleWeatherApiException(
      WeatherApiException ex, HttpServletRequest request) {
    log.error("Erro na API de clima em {}: {}", request.getRequestURI(), ex.getMessage());
    return errorResponse(
        HttpStatus.SERVICE_UNAVAILABLE, "Serviço de Previsão do Tempo Indisponível", ex.getMessage());
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
}
