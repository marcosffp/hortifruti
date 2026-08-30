package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.CapturaIniciadaResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.CapturaPendenteResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.ManualPurchaseRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.NotaExtracaoResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.NotaUploadResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseResponse;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.service.purchase.CapturaNotaPendenteService;
import com.hortifruti.sl.hortifruti.service.purchase.GeminiExtractionService;
import com.hortifruti.sl.hortifruti.service.purchase.NotaUploadService;
import com.hortifruti.sl.hortifruti.service.purchase.tabelapreco.NotaPrecoOficialChecker;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/compras/notas")
@RequiredArgsConstructor
public class NotaController {

  private final NotaUploadService notaUploadService;
  private final GeminiExtractionService geminiExtractionService;
  private final CapturaNotaPendenteService capturaNotaPendenteService;
  private final NotaPrecoOficialChecker notaPrecoOficialChecker;

  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NotaUploadResponse> upload(@RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(notaUploadService.upload(file));
  }

  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @PostMapping(value = "/extrair", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NotaExtracaoResponse> extrair(@RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(geminiExtractionService.extrair(file));
  }

  /**
   * Endpoint do dispositivo vinculado (celular): recebe a foto e responde na hora, sem esperar a
   * extração do Gemini — ver {@code CapturaExtracaoAsyncService}. Aceita também {@code MANAGER}/
   * {@code EMPLOYEE} autenticados normalmente, pra permitir testar sem precisar de um celular
   * pareado.
   */
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE', 'DEVICE_CAPTURE')")
  @PostMapping(value = "/capturas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<CapturaIniciadaResponse> capturar(
      @RequestParam("file") MultipartFile file) {
    CapturaIniciadaResponse resposta =
        capturaNotaPendenteService.receberCaptura(
            file, usuarioAutenticadoId(), dispositivoIdAutenticado());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(resposta);
  }

  /**
   * Só para o PC (login normal) — um dispositivo vinculado nunca deveria conseguir listar a fila
   * inteira do usuário, só enviar novas capturas (ver escopo de {@code ROLE_DEVICE_CAPTURE} em
   * {@code DeviceTokenAuthFilter}).
   */
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @GetMapping("/pendentes")
  public ResponseEntity<List<CapturaPendenteResponse>> pendentes() {
    return ResponseEntity.ok(capturaNotaPendenteService.listarPendentes(usuarioAutenticadoId()));
  }

  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @PostMapping("/pendentes/{id}/descartar")
  public ResponseEntity<Void> descartar(@PathVariable Long id) {
    capturaNotaPendenteService.descartar(id, usuarioAutenticadoId());
    return ResponseEntity.noContent().build();
  }

  /**
   * Tenta a extração de novo pra uma captura que falhou (status ERRO), sem exigir que o usuário
   * tire/envie a foto de novo — a imagem original já está guardada no R2 desde o upload. Resposta
   * imediata (202), igual a {@code /capturas}: a extração roda em segundo plano e o front descobre
   * o resultado pelo mesmo mecanismo de tempo real já usado pra fila.
   */
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @PostMapping("/pendentes/{id}/reprocessar")
  public ResponseEntity<Void> reprocessar(@PathVariable Long id) {
    capturaNotaPendenteService.reprocessar(id, usuarioAutenticadoId());
    return ResponseEntity.accepted().build();
  }

  /**
   * Confirma a captura revisada como uma compra real — ver {@code
   * CapturaNotaPendenteService#confirmarComoCompra} para a decisão de manter ou descartar a foto.
   */
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @PostMapping(value = "/pendentes/{id}/confirmar", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PurchaseResponse> confirmarCaptura(
      @PathVariable Long id, @Valid @RequestBody ManualPurchaseRequest request) {
    return ResponseEntity.ok(
        capturaNotaPendenteService.confirmarComoCompra(id, usuarioAutenticadoId(), request));
  }

  /**
   * Preços oficiais confirmados do cliente pra essa data, por código de produto do catálogo — a
   * extração já aplica isso uma vez com o cliente sugerido pelo Gemini, mas não reprocessa a nota
   * inteira se o usuário trocar o cliente ou o produto de uma linha na tela de revisão; o front usa
   * esse endpoint pra sincronizar o preço nesses casos.
   */
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @GetMapping("/tabela-preco-vigente")
  public ResponseEntity<Map<String, BigDecimal>> tabelaPrecoVigente(
      @RequestParam Long clienteId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
    return ResponseEntity.ok(
        notaPrecoOficialChecker.precosVigentesPorCodigoProduto(clienteId, data));
  }

  /** Foto original da captura, pra tela de revisão comparar lado a lado com a extração. */
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @GetMapping("/pendentes/{id}/imagem")
  public ResponseEntity<byte[]> imagem(@PathVariable Long id) {
    CapturaNotaPendenteService.ImagemCaptura imagem =
        capturaNotaPendenteService.buscarImagem(id, usuarioAutenticadoId());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(imagem.contentType()))
        .body(imagem.bytes());
  }

  private Long usuarioAutenticadoId() {
    User usuario = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return usuario.getId();
  }

  private Long dispositivoIdAutenticado() {
    Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
    return details instanceof Long dispositivoId ? dispositivoId : null;
  }
}
