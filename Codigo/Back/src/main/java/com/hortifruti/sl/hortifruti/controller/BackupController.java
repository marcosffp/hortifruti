package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.BackupResponse;
import com.hortifruti.sl.hortifruti.service.backup.BackupService;
import com.hortifruti.sl.hortifruti.service.backup.oauth.GoogleOAuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://plf-es-2025-2-ti4-1247100-hortifruti-sl-production.up.railway.app"})
public class BackupController {

  private final BackupService backupService;
  private final GoogleOAuthService googleOAuthService;

  @Value("${frontend.url:http://localhost:3000}")
  private String frontendUrl;

  @PostMapping
  public ResponseEntity<BackupResponse> performBackup(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    try {
      log.info("Iniciando backup para período: {} - {}", startDate, endDate);
      BackupResponse response = backupService.handleBackupRequestWithAuthLink(startDate, endDate);
      log.info("Backup concluído com sucesso");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Erro ao executar backup", e);
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
  public ResponseEntity<String> handleOAuth2Callback(
      @RequestParam("code") String authorizationCode,
      HttpServletResponse response) {
    try {
      log.info("Processando callback OAuth2 com código: {}", authorizationCode.substring(0, 10) + "...");
      
      googleOAuthService.handleOAuth2Callback(authorizationCode);
      log.info("Callback OAuth2 processado com sucesso");
      
      // Usar a URL do frontend configurada
      String redirectUrl = frontendUrl + "/backup?auth=success";
      log.info("Redirecionando para: {}", redirectUrl);
      
      response.sendRedirect(redirectUrl);
      
      return ResponseEntity.ok("Redirecionando para o frontend...");
    } catch (Exception e) {
      log.error("Erro ao processar callback OAuth2", e);
      
      try {
        String redirectUrl = frontendUrl + "/backup?auth=error&message=" + 
                           URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        log.info("Redirecionando para URL de erro: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
      } catch (IOException ioException) {
        log.error("Erro no redirecionamento", ioException);
      }
      
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erro ao processar o callback de autorização: " + e.getMessage());
    }
  }
}
