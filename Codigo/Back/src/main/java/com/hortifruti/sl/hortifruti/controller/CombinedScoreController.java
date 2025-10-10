package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import com.hortifruti.sl.hortifruti.service.CombinedScoreSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/grouped-products")
@Tag(name = "CombinedScore Management", description = "Operações relacionadas ao gerenciamento de scores combinados")
public class CombinedScoreController {

  private final CombinedScoreService combinedScoreService;
  private final CombinedScoreSchedulerService combinedScoreSchedulerService;

  @PostMapping("/confirm")
  public ResponseEntity<CombinedScoreResponse> confirmGrouping(
      @Valid @RequestBody CombinedScoreRequest request) {
    CombinedScoreResponse response = combinedScoreService.confirmGrouping(request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> cancelGrouping(@PathVariable Long id) {
    combinedScoreService.cancelGrouping(id);
    return ResponseEntity.ok().body("Agrupamento cancelado com sucesso.");
  }

  @GetMapping
  public ResponseEntity<Page<CombinedScoreResponse>> listGroupings(
      @RequestParam(required = false) Long clientId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<CombinedScoreResponse> response = combinedScoreService.listGroupings(clientId, pageable);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/check-overdue")
  @Operation(
      summary = "Verificação manual de CombinedScores vencidos",
      description = "Executa verificação imediata de CombinedScores com pagamentos vencidos e envia notificações por email e WhatsApp"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Verificação executada com sucesso"),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
  })
  public ResponseEntity<String> checkOverdueCombinedScores() {
    try {
      log.info("Iniciando verificação manual de CombinedScores vencidos");
      
      combinedScoreSchedulerService.manualOverdueCheck();
      
      return ResponseEntity.ok("Verificação de CombinedScores vencidos executada com sucesso. Verifique os logs para detalhes das notificações enviadas.");
      
    } catch (Exception e) {
      log.error("Erro durante verificação manual de CombinedScores vencidos", e);
      return ResponseEntity.status(500).body("Erro durante verificação: " + e.getMessage());
    }
  }
}
