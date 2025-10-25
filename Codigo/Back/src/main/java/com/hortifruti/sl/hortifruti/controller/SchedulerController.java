package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.service.scheduler.ApiTokenService;
import com.hortifruti.sl.hortifruti.service.scheduler.CombinedScoreSchedulerService;
import com.hortifruti.sl.hortifruti.service.scheduler.DatabaseStorageSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

  private final CombinedScoreSchedulerService combinedScoreSchedulerService;
  private final DatabaseStorageSchedulerService databaseStorageSchedulerService;
  private final ApiTokenService apiTokenService;

  /**
   * Endpoint para executar a verificação de CombinedScores vencidos manualmente. Requer token de
   * autenticação específico para APIs programáticas.
   */
  @PostMapping("/check-overdue")
  public ResponseEntity<String> checkOverdueCombinedScores(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Validação do token
    if (!isValidToken(authHeader)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Token de autenticação inválido ou não fornecido");
    }

    combinedScoreSchedulerService.scheduledOverdueCheck();
    return ResponseEntity.ok("Verificação de CombinedScores vencidos iniciada com sucesso.");
  }

  /**
   * Endpoint para executar a verificação de armazenamento do banco de dados manualmente. Requer
   * token de autenticação específico para APIs programáticas.
   */
  @PostMapping("/check-database-storage")
  public ResponseEntity<String> checkDatabaseStorage(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Validação do token
    if (!isValidToken(authHeader)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Token de autenticação inválido ou não fornecido");
    }

    databaseStorageSchedulerService.scheduledDatabaseCheck();
    return ResponseEntity.ok(
        "Verificação de armazenamento do banco de dados iniciada com sucesso.");
  }

  /**
   * Endpoint para executar o backup. Requer token de autenticação específico para APIs
   * programáticas.
   */
  @PostMapping("/perform-backup")
  public ResponseEntity<String> performBackup(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Validação do token
    if (!isValidToken(authHeader)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Token de autenticação inválido ou não fornecido");
    }

    return ResponseEntity.ok("Backup iniciado com sucesso.");
  }

  /**
   * Método auxiliar para validar o token de autenticação. Extrai o token do header "Authorization"
   * (remove o prefixo "Bearer ").
   */
  private boolean isValidToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return false;
    }

    String token = authHeader.substring(7); // Remove "Bearer "
    return apiTokenService.validateSchedulerToken(token);
  }
}
