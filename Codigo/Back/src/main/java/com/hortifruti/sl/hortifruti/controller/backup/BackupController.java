package com.hortifruti.sl.hortifruti.controller.backup;

import com.hortifruti.sl.hortifruti.dto.backup.BackupResponse;
import com.hortifruti.sl.hortifruti.service.backup.BackupService;
import com.hortifruti.sl.hortifruti.service.backup.oauth.GoogleOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/backup")
@AllArgsConstructor
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
      log.error("Erro ao executar backup para o período {} a {}", startDate, endDate, e);
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

  /**
   * Callback do OAuth2 do Google, compartilhado pelos fluxos de backup (Drive) e de envio de email
   * (Gmail API) — o parâmetro {@code state} identifica qual dos dois iniciou a autorização, para
   * redirecionar de volta à tela correta.
   */
  @GetMapping("/oauth2callback")
  public void handleOAuth2Callback(
      @RequestParam("code") String authorizationCode,
      @RequestParam(required = false) String state,
      HttpServletResponse response) {
    googleOAuthService.processOAuth2Callback(authorizationCode, state, response);
  }
}
