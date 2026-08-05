package com.hortifruti.sl.hortifruti.controller.device;

import com.hortifruti.sl.hortifruti.config.auth.DispositivoVinculadoService;
import com.hortifruti.sl.hortifruti.dto.device.ConfirmarPareamentoRequest;
import com.hortifruti.sl.hortifruti.dto.device.ConfirmarPareamentoResponse;
import com.hortifruti.sl.hortifruti.dto.device.DispositivoResponse;
import com.hortifruti.sl.hortifruti.dto.device.IniciarPareamentoResponse;
import com.hortifruti.sl.hortifruti.model.User;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

  private final DispositivoVinculadoService dispositivoVinculadoService;

  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @PostMapping("/pareamento/iniciar")
  public ResponseEntity<IniciarPareamentoResponse> iniciarPareamento() {
    return ResponseEntity.ok(
        dispositivoVinculadoService.gerarCodigoPareamento(usuarioAutenticadoId()));
  }

  /**
   * Público: quem chama é o navegador do celular, que ainda não tem nenhum credencial nesse momento
   * — a segurança deste endpoint é o código de 6 dígitos de vida curta e uso único, não uma sessão
   * (liberado explicitamente em SecurityConfig).
   */
  @PostMapping("/pareamento/confirmar")
  public ResponseEntity<ConfirmarPareamentoResponse> confirmarPareamento(
      @Valid @RequestBody ConfirmarPareamentoRequest request) {
    return ResponseEntity.ok(
        dispositivoVinculadoService.confirmarPareamento(
            request.codigo(), request.nomeDispositivo()));
  }

  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @GetMapping
  public ResponseEntity<List<DispositivoResponse>> listar() {
    return ResponseEntity.ok(
        dispositivoVinculadoService.listarDispositivos(usuarioAutenticadoId()));
  }

  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> revogar(@PathVariable Long id) {
    dispositivoVinculadoService.revogar(id, usuarioAutenticadoId());
    return ResponseEntity.noContent().build();
  }

  private Long usuarioAutenticadoId() {
    User usuario = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return usuario.getId();
  }
}
