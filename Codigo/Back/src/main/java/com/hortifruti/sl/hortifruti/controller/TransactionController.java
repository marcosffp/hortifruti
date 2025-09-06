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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionProcessingService transactionProcessingService;
  private final TransactionExcelExportService transactionExcelExportService;

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> importStatements(@RequestParam("file") MultipartFile file) {
    try {
      if (file == null || file.isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nenhum arquivo foi enviado.");
      }
      System.out.println("Arquivo recebido: " + file.getOriginalFilename());
      transactionProcessingService.processFileAsync(file);
      return ResponseEntity.status(HttpStatus.ACCEPTED).body("Processamento iniciado.");
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erro ao iniciar processamento: " + e.getMessage());
    }
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
