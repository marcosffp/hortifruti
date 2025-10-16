package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.service.DashboardService;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@AllArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  /**
   * Endpoint para obter os dados do dashboard.
   *
   * @param startDate Data de início do período (formato: yyyy-MM-dd).
   * @param endDate Data de fim do período (formato: yyyy-MM-dd).
   * @param month Mês para o ranking de categorias de gastos.
   * @param year Ano para o ranking de categorias de gastos.
   * @return Um objeto contendo todas as divisórias do dashboard.
   */
  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping
  public ResponseEntity<Map<String, Object>> getDashboardData(
      @RequestParam("startDate") String startDate,
      @RequestParam("endDate") String endDate,
      @RequestParam("month") int month,
      @RequestParam("year") int year) {

    // Converte os parâmetros para os tipos necessários
    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);
    Month selectedMonth = Month.of(month);

    // Chama o serviço para obter os dados do dashboard
    Map<String, Object> dashboardData =
        dashboardService.getDashboardData(start, end, selectedMonth, year);

    // Retorna os dados no corpo da resposta
    return ResponseEntity.ok(dashboardData);
  }
}
