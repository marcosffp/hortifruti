package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.transaction.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.transaction.TransactionResponse;
import com.hortifruti.sl.hortifruti.service.transaction.TransactionExcelExportService;
import com.hortifruti.sl.hortifruti.service.transaction.TransactionProcessingService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
  private final TransactionExcelExportService transactionExcelExportService;

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/revenue")
  public ResponseEntity<BigDecimal> getTotalRevenueForCurrentMonth() {
    BigDecimal totalRevenue = transactionProcessingService.getTotalRevenueForCurrentMonth();
    return ResponseEntity.ok(totalRevenue);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/expenses")
  public ResponseEntity<BigDecimal> getTotalExpensesForCurrentMonth() {
    BigDecimal totalExpenses = transactionProcessingService.getTotalExpensesForCurrentMonth();
    return ResponseEntity.ok(totalExpenses);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/balance")
  public ResponseEntity<BigDecimal> getTotalBalanceForCurrentMonth() {
    BigDecimal totalBalance = transactionProcessingService.getTotalBalanceForCurrentMonth();
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
      @RequestParam(defaultValue = "10") int size) {
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
  public ResponseEntity<byte[]> exportTransactionsAsExcel() throws IOException {
    // Gerar o arquivo Excel
    Map<String, byte[]> excelData = transactionExcelExportService.exportTransactionsAsExcel();
    String excelFileName = excelData.keySet().iterator().next();
    byte[] excelFile = excelData.get(excelFileName);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + excelFileName)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelFile);
  }
}
