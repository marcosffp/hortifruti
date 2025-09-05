package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.TransactionResponse;
import com.hortifruti.sl.hortifruti.service.TransactionExcelExportService;
import com.hortifruti.sl.hortifruti.service.TransactionProcessingService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionProcessingService transactionProcessingService;
  private final TransactionExcelExportService transactionExcelExportService;

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> importStatements(@RequestPart("files") List<MultipartFile> files)
      throws IOException {
    transactionProcessingService.importStatements(files);
    return ResponseEntity.status(HttpStatus.CREATED).body("Transações importadas com sucesso.");
  }

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

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping
  public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
    List<TransactionResponse> transactions = transactionProcessingService.getAllTransactions();
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
  @PostMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> exportTransactionsAsExcel() throws IOException {
    // Gerar o arquivo Excel
    byte[] excelFile = transactionExcelExportService.exportTransactionsAsZip();

    // Nome do arquivo Excel
    String currentMonth =
        LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
    String excelFileName = "Planilha-Hortifruti-Santa-Luzia-" + currentMonth + ".xlsx";

    // Configurar o cabeçalho da resposta
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + excelFileName);

    return ResponseEntity.ok()
        .headers(headers)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelFile);
  }
}
