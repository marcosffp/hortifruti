package com.hortifruti.sl.hortifruti.controller.finance;

import com.hortifruti.sl.hortifruti.dto.bb.BBImportSummary;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobImportSummary;
import com.hortifruti.sl.hortifruti.dto.transaction.StatementResponse;
import com.hortifruti.sl.hortifruti.service.finance.StatementService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statements")
@RequiredArgsConstructor
public class StatementController {
  private final StatementService statementService;

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping
  public ResponseEntity<List<StatementResponse>> list() {
    return ResponseEntity.ok(statementService.listAll());
  }

  /**
   * Consulta o extrato do Sicoob via API (mês/ano, com dia inicial/final opcionais), gera o PDF
   * correspondente, guarda o PDF e as transações novas.
   */
  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping("/sicoob-api/import")
  public ResponseEntity<SicoobImportSummary> importFromSicoobApi(
      @RequestParam int mes,
      @RequestParam int ano,
      @RequestParam(required = false) Integer diaInicial,
      @RequestParam(required = false) Integer diaFinal)
      throws IOException {
    return ResponseEntity.ok(statementService.importFromSicoobApi(mes, ano, diaInicial, diaFinal));
  }

  /** Baixa o extrato do período (via API do Sicoob) em PDF, no layout do extrato original. */
  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/sicoob-api/export/pdf")
  public ResponseEntity<byte[]> exportSicoobExtratoPdf(
      @RequestParam int mes,
      @RequestParam int ano,
      @RequestParam(required = false) Integer diaInicial,
      @RequestParam(required = false) Integer diaFinal)
      throws IOException {
    byte[] pdf = statementService.exportSicoobExtratoPdf(mes, ano, diaInicial, diaFinal);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName(mes, ano, "pdf"))
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  /** Baixa o extrato do período (via API do Sicoob) em Excel, no layout do extrato original. */
  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/sicoob-api/export/excel")
  public ResponseEntity<byte[]> exportSicoobExtratoExcel(
      @RequestParam int mes,
      @RequestParam int ano,
      @RequestParam(required = false) Integer diaInicial,
      @RequestParam(required = false) Integer diaFinal)
      throws IOException {
    byte[] excel = statementService.exportSicoobExtratoExcel(mes, ano, diaInicial, diaFinal);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName(mes, ano, "xlsx"))
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excel);
  }

  private String fileName(int mes, int ano, String extensao) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    return String.format("extrato-conta-corrente_%d%02d_%s.%s", ano, mes, timestamp, extensao);
  }

  /**
   * Consulta o extrato do BB via API (dataInicio/dataFim, período máximo de 31 dias segundo a API),
   * gera o PDF correspondente, guarda o PDF e as transações novas.
   */
  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping("/bb-api/import")
  public ResponseEntity<BBImportSummary> importFromBBApi(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
      throws IOException {
    return ResponseEntity.ok(statementService.importFromBBApi(dataInicio, dataFim));
  }

  /** Baixa o extrato do período (via API do BB) em PDF, no layout do extrato original. */
  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/bb-api/export/pdf")
  public ResponseEntity<byte[]> exportBBExtratoPdf(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
      throws IOException {
    byte[] pdf = statementService.exportBBExtratoPdf(dataInicio, dataFim);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=" + fileNameBB(dataInicio, dataFim, "pdf"))
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  /** Baixa o extrato do período (via API do BB) em Excel, no layout do extrato original. */
  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/bb-api/export/excel")
  public ResponseEntity<byte[]> exportBBExtratoExcel(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim)
      throws IOException {
    byte[] excel = statementService.exportBBExtratoExcel(dataInicio, dataFim);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=" + fileNameBB(dataInicio, dataFim, "xlsx"))
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excel);
  }

  private String fileNameBB(LocalDate dataInicio, LocalDate dataFim, String extensao) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
    return String.format(
        "extrato-bb_%s_a_%s.%s", dataInicio.format(fmt), dataFim.format(fmt), extensao);
  }
}
