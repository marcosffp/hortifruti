package com.hortifruti.sl.hortifruti.controller.product;

import com.hortifruti.sl.hortifruti.dto.product.ConversaoCaixaImportResponse;
import com.hortifruti.sl.hortifruti.service.product.ConversaoCaixaImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/produtos/conversao-caixa")
@RequiredArgsConstructor
public class ConversaoCaixaController {

  private final ConversaoCaixaImportService conversaoCaixaImportService;

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ConversaoCaixaImportResponse> importar(
      @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(conversaoCaixaImportService.importar(file));
  }
}
