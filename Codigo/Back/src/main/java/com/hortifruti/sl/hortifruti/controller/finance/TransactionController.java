package com.hortifruti.sl.hortifruti.controller.finance;

import com.hortifruti.sl.hortifruti.dto.finance.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.finance.TransactionRequestDate;
import com.hortifruti.sl.hortifruti.dto.finance.TransactionResponse;
import com.hortifruti.sl.hortifruti.service.finance.MacroExportService;
import com.hortifruti.sl.hortifruti.service.finance.transaction.TransactionProcessingService;
import com.hortifruti.sl.hortifruti.service.finance.transaction.TransactionReportService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionProcessingService transactionProcessingService;
  private final MacroExportService macroExportService;
  private final TransactionReportService transactionReportService;

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/revenue")
  public ResponseEntity<BigDecimal> getTotalRevenue(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    TransactionRequestDate request = new TransactionRequestDate(startDate, endDate);
    BigDecimal totalRevenue = transactionProcessingService.getTotalRevenue(request);
    return ResponseEntity.ok(totalRevenue);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/expenses")
  public ResponseEntity<BigDecimal> getTotalExpenses(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    TransactionRequestDate request = new TransactionRequestDate(startDate, endDate);
    BigDecimal totalExpenses = transactionProcessingService.getTotalExpenses(request);
    return ResponseEntity.ok(totalExpenses);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/balance")
  public ResponseEntity<BigDecimal> getTotalBalance(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    TransactionRequestDate request = new TransactionRequestDate(startDate, endDate);
    BigDecimal totalBalance = transactionProcessingService.getTotalBalance(request);
    return ResponseEntity.ok(totalBalance);
  }

  @GetMapping("/categories")
  public ResponseEntity<List<String>> getAllCategories() {
    List<String> categories = transactionProcessingService.getAllCategories();
    return ResponseEntity.ok(categories);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping
  public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<TransactionResponse> transactions =
        transactionProcessingService.getAllTransactions(search, type, category, page, size);
    return ResponseEntity.ok(transactions);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @PutMapping("/{id}")
  public ResponseEntity<TransactionResponse> updateTransaction(
      @PathVariable Long id, @Valid @RequestBody TransactionRequest transactionRequest) {
    TransactionResponse updatedResponse =
        transactionProcessingService.updateTransaction(id, transactionRequest);
    return ResponseEntity.ok(updatedResponse);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
    transactionProcessingService.deleteTransaction(id);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(
      value = "/export",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> exportTransactionsAsExcel(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate)
      throws IOException {
    byte[] excelFile = transactionReportService.generateExcel(startDate, endDate);
    String excelFileName = transactionReportService.buildFileName(startDate, endDate, "xlsx");

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + excelFileName)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelFile);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/export-complete", produces = "application/zip")
  public ResponseEntity<byte[]> exportTransactionsComplete() throws IOException {
    Map<String, byte[]> zipData = macroExportService.exportMacroReports();
    String zipFileName = zipData.keySet().iterator().next();
    byte[] zipFile = zipData.get(zipFileName);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName)
        .contentType(MediaType.parseMediaType("application/zip"))
        .body(zipFile);
  }

  /**
   * Relatório consolidado de transações (BB + Sicoob juntos, PDF_UPLOAD e API) em PDF, em ordem
   * cronológica, com descrição e categoria de cada lançamento. Sem startDate/endDate, usa o mês
   * anterior completo, mesmo padrão de {@link #exportTransactionsAsExcel}.
   */
  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping(value = "/report/pdf")
  public ResponseEntity<byte[]> exportTransactionsReportPdf(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate)
      throws IOException {
    byte[] pdf = transactionReportService.generatePdf(startDate, endDate);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=" + reportFileName(startDate, endDate, "pdf"))
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  /** Mesmo relatório consolidado de {@link #exportTransactionsReportPdf}, em Excel. */
  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping(value = "/report/excel")
  public ResponseEntity<byte[]> exportTransactionsReportExcel(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate)
      throws IOException {
    byte[] excel = transactionReportService.generateExcel(startDate, endDate);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=" + reportFileName(startDate, endDate, "xlsx"))
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excel);
  }

  private String reportFileName(LocalDate startDate, LocalDate endDate, String extensao) {
    LocalDate now = LocalDate.now();
    LocalDate inicio = startDate != null ? startDate : now.minusMonths(1).withDayOfMonth(1);
    LocalDate fim = endDate != null ? endDate : now.withDayOfMonth(1).minusDays(1);
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
    return String.format(
        "relatorio-transacoes_%s_a_%s.%s", inicio.format(fmt), fim.format(fmt), extensao);
  }
}
