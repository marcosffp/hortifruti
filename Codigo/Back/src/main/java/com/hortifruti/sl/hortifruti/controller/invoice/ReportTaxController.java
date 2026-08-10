package com.hortifruti.sl.hortifruti.controller.invoice;

import com.hortifruti.sl.hortifruti.service.invoice.tax.ReportTaxService;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@AllArgsConstructor
public class ReportTaxController {

  private final ReportTaxService reportTaxService;

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/icms-report/monthly/{start}/{end}")
  public ResponseEntity<byte[]> generateMonthlyReports(
      @PathVariable LocalDate start, @PathVariable LocalDate end) {
    byte[] zipBytes = reportTaxService.generateMonthly(start, end);

    if (zipBytes == null || zipBytes.length == 0) {
      return ResponseEntity.badRequest()
          .body("Erro: Não foi possível gerar os relatórios".getBytes());
    }

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorios_mensais.zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(zipBytes);
  }
}
