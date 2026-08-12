package com.hortifruti.sl.hortifruti.controller.dashboard;

import com.hortifruti.sl.hortifruti.dto.dashboard.DashboardResponse;
import com.hortifruti.sl.hortifruti.service.dashboard.DashboardService;
import java.time.LocalDate;
import java.time.Month;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@AllArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping
  public ResponseEntity<DashboardResponse> getDashboardData(
      @RequestParam("startDate") String startDate,
      @RequestParam("endDate") String endDate,
      @RequestParam("month") int month,
      @RequestParam("year") int year) {

    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);
    Month selectedMonth = Month.of(month);

    DashboardResponse dashboardData =
        dashboardService.getDashboardData(start, end, selectedMonth, year);

    return ResponseEntity.ok(dashboardData);
  }
}
