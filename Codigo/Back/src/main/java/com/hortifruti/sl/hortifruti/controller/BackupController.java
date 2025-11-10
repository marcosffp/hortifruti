package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.BackupResponse;
import com.hortifruti.sl.hortifruti.service.backup.BackupService;
import com.hortifruti.sl.hortifruti.service.backup.oauth.GoogleOAuthService;

import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://plf-es-2025-2-ti4-1247100-hortifruti-sl-production.up.railway.app"})
public class BackupController {

  private final BackupService backupService;
  private final GoogleOAuthService googleOAuthService;

  @PreAuthorize("hasRole('MANAGER')")
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

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/storage")
  public ResponseEntity<BackupResponse> getDatabaseStorage() {
    BigDecimal databaseSize = backupService.getDatabaseSizeInMB();
    return ResponseEntity.ok(
        new BackupResponse(databaseSize + "/" + backupService.getMaxDatabaseSizeInMB() + " MB"));
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/oauth2callback")
  public ResponseEntity<String> handleOAuth2Callback(
      @RequestParam("code") String authorizationCode,
      HttpServletResponse response) {
    try {
      googleOAuthService.handleOAuth2Callback(authorizationCode);
      
      // Redirecionar para o frontend após sucesso
      String frontendUrl = "http://localhost:3000/backup?auth=success";
      response.sendRedirect(frontendUrl);
      
      return ResponseEntity.ok("Redirecionando para o frontend...");
    } catch (Exception e) {
      try {
        String frontendUrl = "http://localhost:3000/backup?auth=error&message=" + 
                           URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl);
      } catch (IOException ioException) {
        // fallback
      }
      
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erro ao processar o callback de autorização: " + e.getMessage());
    }
  }
}
