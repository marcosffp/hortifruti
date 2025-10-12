package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.service.backup.BackupService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backup")
@AllArgsConstructor
public class BackupController {

  private final BackupService backupService;

  @PostMapping("/manual")
  public ResponseEntity<String> triggerManualBackup() {
    String result = backupService.performManualBackup();
    return ResponseEntity.ok(result);
  }
}
