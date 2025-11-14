package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.BackupResponse;
import com.hortifruti.sl.hortifruti.service.backup.BackupService;
import com.hortifruti.sl.hortifruti.service.backup.oauth.GoogleOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/backup")
@AllArgsConstructor
public class BackupController {

  private final BackupService backupService;
  private final GoogleOAuthService googleOAuthService;

  @PostMapping
  public ResponseEntity<BackupResponse> performBackup(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    try {
      BackupResponse response = backupService.handleBackupRequestWithAuthLink(startDate, endDate);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new BackupResponse("Erro: " + e.getMessage()));
    }
  }

  @GetMapping("/storage")
  public ResponseEntity<BackupResponse> getDatabaseStorage() {
    BigDecimal databaseSize = backupService.getDatabaseSizeInMB();
    return ResponseEntity.ok(
        new BackupResponse(databaseSize + "/" + backupService.getMaxDatabaseSizeInMB() + " MB"));
  }

  @GetMapping("/oauth2callback")
  public void handleOAuth2Callback(
      @RequestParam("code") String authorizationCode, HttpServletResponse response) {
    googleOAuthService.processOAuth2Callback(authorizationCode, response);
  }
}
