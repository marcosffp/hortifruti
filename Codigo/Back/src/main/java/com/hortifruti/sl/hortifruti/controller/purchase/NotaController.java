package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.CapturaIniciadaResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.CapturaPendenteResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.NotaExtracaoResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.NotaUploadResponse;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.service.purchase.CapturaNotaPendenteService;
import com.hortifruti.sl.hortifruti.service.purchase.GeminiExtractionService;
import com.hortifruti.sl.hortifruti.service.purchase.NotaUploadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
