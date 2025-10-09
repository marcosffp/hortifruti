package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.transaction.StatementResponse;
import com.hortifruti.sl.hortifruti.service.transaction.StatementService;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/statements")
@RequiredArgsConstructor
public class StatementController {
  private final StatementService statementService;

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> upload(@RequestPart("files") MultipartFile[] files)
      throws IOException {
    statementService.saveAll(files);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body("Processamento iniciado.");
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping
  public ResponseEntity<List<StatementResponse>> list() {
    return ResponseEntity.ok(statementService.listAll());
  }
}
