package com.hortifruti.sl.hortifruti.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resposta minima de saldo: nunca inclui agencia, conta, lancamentos ou qualquer outro dado da API
 * do BB alem do valor final, para nao expor mais do que o necessario ao front.
 */
public record BankBalanceResponse(
    BigDecimal saldoDisponivel, boolean detalhado, LocalDateTime consultadoEm) {}
