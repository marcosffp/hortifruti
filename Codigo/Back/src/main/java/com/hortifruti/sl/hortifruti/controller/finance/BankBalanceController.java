package com.hortifruti.sl.hortifruti.controller.finance;

import com.hortifruti.sl.hortifruti.dto.finance.BankBalanceResponse;
import com.hortifruti.sl.hortifruti.service.finance.BBSaldoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint somente-leitura e sem parametros: sempre devolve o saldo da conta corrente configurada
 * no servidor (Codigo/Back/.env), nunca de uma conta escolhida pelo chamador.
 */
@RestController
@RequestMapping("/finance/bank-balance")
@RequiredArgsConstructor
public class BankBalanceController {

  private final BBSaldoService bbSaldoService;

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping
  public ResponseEntity<BankBalanceResponse> getSaldoDisponivel() {
    return ResponseEntity.ok(bbSaldoService.getSaldoDisponivel());
  }
}
