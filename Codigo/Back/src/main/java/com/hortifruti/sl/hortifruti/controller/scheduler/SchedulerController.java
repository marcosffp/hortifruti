package com.hortifruti.sl.hortifruti.controller.scheduler;

import com.hortifruti.sl.hortifruti.service.scheduler.CombinedScoreOverdueService;
import com.hortifruti.sl.hortifruti.service.scheduler.DatabaseStorageSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Todas as rotas deste controller são {@code permitAll} no {@link
 * com.hortifruti.sl.hortifruti.config.auth.SecurityConfig} e protegidas por um token estático
 * validado em {@link com.hortifruti.sl.hortifruti.config.auth.SecurityFilter} (não por papel de
 * usuário) — a checagem acontece inteiramente no filtro, então nenhum método aqui precisa revalidar
 * o token.
 */
@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

  private final CombinedScoreOverdueService combinedScoreOverdueService;
  private final DatabaseStorageSchedulerService databaseStorageSchedulerService;

  @GetMapping("/health")
  public ResponseEntity<String> checkHealth() {
    return ResponseEntity.ok("Aplicação está ativa e funcionando corretamente.");
  }

  @PostMapping("/check-overdue")
  public ResponseEntity<String> checkOverdueCombinedScores() {
    combinedScoreOverdueService.scheduledOverdueCheck();
    return ResponseEntity.ok("Verificação de CombinedScores vencidos iniciada com sucesso.");
  }

  @PostMapping("/check-database-storage")
  public ResponseEntity<String> checkDatabaseStorage() {
    databaseStorageSchedulerService.scheduledDatabaseCheck();
    return ResponseEntity.ok(
        "Verificação de armazenamento do banco de dados iniciada com sucesso.");
  }
}
