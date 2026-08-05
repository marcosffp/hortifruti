package com.hortifruti.sl.hortifruti.controller.realtime;

import com.hortifruti.sl.hortifruti.dto.realtime.RealtimeTicketResponse;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.service.realtime.RealtimeTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chamado pelo front autenticado por cookie (same-origin, via {@code /api}) antes de abrir o
 * WebSocket de tempo real — ver {@link RealtimeTicketService} para o motivo do ticket existir.
 */
@RestController
@RequestMapping("/realtime")
@RequiredArgsConstructor
public class RealtimeTicketController {

  private final RealtimeTicketService ticketService;

  @PostMapping("/ws-ticket")
  public ResponseEntity<RealtimeTicketResponse> emitirTicket() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication != null && authentication.getPrincipal() instanceof User usuario)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    return ResponseEntity.ok(new RealtimeTicketResponse(ticketService.emitir(usuario.getId())));
  }
}
