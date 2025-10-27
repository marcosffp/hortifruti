package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.BackupResponse;
import com.hortifruti.sl.hortifruti.service.backup.BackupService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

  private final BackupService backupService;

  /**
   * Endpoint para realizar o backup completo ou por período.
   *
   * @param startDate Data inicial do período (formato ISO, opcional).
   * @param endDate Data final do período (formato ISO, opcional).
   * @return Mensagem de sucesso ou erro.
   */
  @PostMapping
  public ResponseEntity<BackupResponse> performBackup(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(backupService.handleBackupRequestWithAuthLink(startDate, endDate));
  }

  /**
   * Endpoint para obter o tamanho atual do banco de dados.
   *
   * @return Tamanho do banco de dados em MB.
   */
  @GetMapping("/storage")
  public ResponseEntity<BackupResponse> getDatabaseStorage() {
    BigDecimal databaseSize = backupService.getDatabaseSizeInMB();
    return ResponseEntity.ok(new BackupResponse( databaseSize + " MB"));
  }
}
