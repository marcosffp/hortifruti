package com.hortifruti.sl.hortifruti.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<Map<String, String>> handleAuthException(AuthException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de Autenticação");
    response.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(TokenException.class)
  public ResponseEntity<Map<String, String>> handleTokenException(TokenException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de Token");
    response.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationException(
      MethodArgumentNotValidException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de validação");

    if (!ex.getBindingResult().getFieldErrors().isEmpty()) {
      String firstErrorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
      response.put("message", firstErrorMessage);
    } else {
      response.put(
          "message", "Dados fornecidos são inválidos. Por favor, verifique e tente novamente.");
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de validação");

    String errorMessage = ex.getMessage();
    if (errorMessage != null && errorMessage.contains("Duplicate entry")) {
      if (errorMessage.contains("users")) {
        response.put(
            "message", "Nome de usuário já está em uso. Por favor, escolha outro nome de usuário.");
      } else {
        response.put(
            "message", "Registro duplicado detectado. Por favor, verifique os dados fornecidos.");
      }
      return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    response.put(
        "message",
        "Erro de integridade dos dados. Por favor, verifique as informações fornecidas.");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(StatementException.class)
  public ResponseEntity<Map<String, String>> handleStatementException(StatementException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de Extrato");
    response.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(TransactionException.class)
  public ResponseEntity<Map<String, String>> handleTransactionException(TransactionException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de Transação");
    response.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(ClientException.class)
  public ResponseEntity<Map<String, String>> handleClientException(ClientException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de Cliente");
    response.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(UserException.class)
  public ResponseEntity<Map<String, String>> handleUserException(UserException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de Usuário");
    response.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro interno do servidor");
    response.put(
        "message",
        "Ocorreu um erro ao processar sua solicitação. Por favor, tente novamente mais tarde.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  @ExceptionHandler(org.springframework.dao.DataAccessResourceFailureException.class)
  public ResponseEntity<Map<String, String>> handleDatabaseConnectionException(
      org.springframework.dao.DataAccessResourceFailureException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de Conexão com o Banco de Dados");
    response.put(
        "message",
        "Não foi possível conectar ao banco de dados. Por favor, tente novamente mais tarde.");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @ExceptionHandler(FreightException.class)
  public ResponseEntity<Map<String, String>> handleFreightException(FreightException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de Frete");
    response.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(DistanceException.class)
  public ResponseEntity<Map<String, String>> handleDistanceException(DistanceException ex) {
    Map<String, String> response = new HashMap<>();
    response.put("error", "Erro de Distância");
    response.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }
}
