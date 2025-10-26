package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.service.backup.BackupService;
import lombok.RequiredArgsConstructor;
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
  public ResponseEntity<String> performBackup(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(backupService.handleBackupRequestWithAuthLink(startDate, endDate));
  }
}
