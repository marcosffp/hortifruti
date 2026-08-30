package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.ConfirmarItemTabelaPrecoRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.TabelaPrecoClienteResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.TabelaPrecoImportResponse;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import com.hortifruti.sl.hortifruti.service.purchase.tabelapreco.TabelaPrecoClienteExportService;
import com.hortifruti.sl.hortifruti.service.purchase.tabelapreco.TabelaPrecoClienteImportService;
import com.hortifruti.sl.hortifruti.service.purchase.tabelapreco.TabelaPrecoClientePdfGenerator;
import com.hortifruti.sl.hortifruti.service.purchase.tabelapreco.TabelaPrecoClienteReviewService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Import/revisão/confirmação/exportação da tabela de preços de um cliente por competência — fluxo
 * de back-office, não do dia a dia da equipe, por isso {@code MANAGER}-only em todos os endpoints
 * (mesmo padrão de {@code ConversaoCaixaController}).
 */
@RestController
@RequestMapping("/api/compras/tabelas-preco-cliente")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class TabelaPrecoClienteController {

  private final TabelaPrecoClienteImportService tabelaPrecoClienteImportService;
  private final TabelaPrecoClienteReviewService tabelaPrecoClienteReviewService;
  private final TabelaPrecoClienteExportService tabelaPrecoClienteExportService;
  private final TabelaPrecoClientePdfGenerator tabelaPrecoClientePdfGenerator;
  private final TabelaPrecoClienteRepository tabelaPrecoClienteRepository;

  @PostMapping(
      value = "/clientes/{clienteId}/importar",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<TabelaPrecoImportResponse> importar(
      @PathVariable Long clienteId, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(
        tabelaPrecoClienteImportService.importar(clienteId, file, usuarioAutenticadoId()));
  }

  @GetMapping("/{tabelaId}")
  public ResponseEntity<TabelaPrecoClienteResponse> buscar(@PathVariable Long tabelaId) {
    return ResponseEntity.ok(tabelaPrecoClienteReviewService.buscarTabela(tabelaId));
  }

  /** Todas as competências/versões importadas pra um cliente, mais recente primeiro. */
  @GetMapping("/clientes/{clienteId}")
  public ResponseEntity<List<TabelaPrecoCliente>> listarPorCliente(@PathVariable Long clienteId) {
    return ResponseEntity.ok(
        tabelaPrecoClienteRepository
            .findByClienteIdOrderByCompetenciaAnoDescCompetenciaMesDescVersaoDesc(clienteId));
  }

  @PostMapping("/{tabelaId}/itens/{itemId}/confirmar")
  public ResponseEntity<TabelaPrecoClienteResponse> confirmarItem(
      @PathVariable Long tabelaId,
      @PathVariable Long itemId,
      @RequestBody ConfirmarItemTabelaPrecoRequest request) {
    return ResponseEntity.ok(
        tabelaPrecoClienteReviewService.confirmarItem(
            tabelaId, itemId, request.fiscalProductCode(), usuarioAutenticadoId()));
  }

  @PostMapping("/{tabelaId}/itens/{itemId}/sem-correspondencia")
  public ResponseEntity<TabelaPrecoClienteResponse> marcarSemCorrespondencia(
      @PathVariable Long tabelaId, @PathVariable Long itemId) {
    return ResponseEntity.ok(
        tabelaPrecoClienteReviewService.marcarSemCorrespondencia(tabelaId, itemId));
  }

  @PostMapping("/{tabelaId}/confirmar-lote")
  public ResponseEntity<Integer> confirmarEmLote(@PathVariable Long tabelaId) {
    return ResponseEntity.ok(
        tabelaPrecoClienteReviewService.confirmarEmLote(tabelaId, usuarioAutenticadoId()));
  }

  @PostMapping("/{tabelaId}/confirmar")
  public ResponseEntity<TabelaPrecoClienteResponse> confirmarTabela(@PathVariable Long tabelaId) {
    return ResponseEntity.ok(
        tabelaPrecoClienteReviewService.confirmarTabela(tabelaId, usuarioAutenticadoId()));
  }

  @GetMapping("/{tabelaId}/exportar/csv")
  public ResponseEntity<byte[]> exportarCsv(@PathVariable Long tabelaId) {
    byte[] csv = tabelaPrecoClienteExportService.exportarCsv(tabelaId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("tabela-preco-cliente-" + tabelaId + ".csv")
                .build()
                .toString())
        .body(csv);
  }

  @GetMapping("/{tabelaId}/exportar/pdf")
  public ResponseEntity<byte[]> exportarPdf(@PathVariable Long tabelaId) {
    byte[] pdf;
    try {
      pdf =
          tabelaPrecoClientePdfGenerator.gerarPdf(
              tabelaPrecoClienteExportService.montarLinhas(tabelaId));
    } catch (IOException e) {
      throw new UncheckedIOException("Não foi possível gerar o PDF de exportação.", e);
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("tabela-preco-cliente-" + tabelaId + ".pdf")
                .build()
                .toString())
        .body(pdf);
  }

  private Long usuarioAutenticadoId() {
    User usuario = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return usuario.getId();
  }
}
