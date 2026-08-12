package com.hortifruti.sl.hortifruti.controller.billet;

import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.dto.billet.OpenBilletResponse;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billet")
@AllArgsConstructor
public class BilletController {

  private final BilletService billetService;

  /**
   * @param combinedScoreId ID do CombinedScore
   * @param number Número identificador do boleto
   * @param dueDate Data de vencimento opcional (formato yyyy-MM-dd) — se null, usa a data padrão do
   *     CombinedScore
   */
  @GetMapping("/generate/{combinedScoreId}")
  public ResponseEntity<byte[]> generateBillet(
      @PathVariable Long combinedScoreId,
      @RequestParam String number,
      @RequestParam(required = false) String dueDate)
      throws IOException {
    return billetService.generateBillet(combinedScoreId, number, dueDate);
  }

  /**
   * Lista boletos de um pagador específico. Sem filtros, retorna os boletos em aberto.
   *
   * @param codigoSituacao Situação do boleto (1=Em aberto, 2=Baixado, 3=Liquidado), opcional.
   */
  @GetMapping("/client/{clientId}")
  public ResponseEntity<List<BilletResponse>> listBilletByPayer(
      @PathVariable long clientId,
      @RequestParam(required = false) Integer codigoSituacao,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim)
      throws IOException {
    boolean hasFilters = codigoSituacao != null || dataInicio != null || dataFim != null;
    List<BilletResponse> billets =
        hasFilters
            ? billetService.listBilletByPayer(clientId, codigoSituacao, dataInicio, dataFim)
            : billetService.listBilletByPayer(clientId);
    return ResponseEntity.ok(billets);
  }

  /** Lista todos os boletos em aberto de todos os clientes, ordenados por data de vencimento. */
  @GetMapping("/open")
  public ResponseEntity<List<OpenBilletResponse>> listAllOpenBillets() {
    return ResponseEntity.ok(billetService.listAllOpenBillets());
  }

  @GetMapping("/issue-copy/{combinedScoreId}")
  public ResponseEntity<byte[]> issueCopy(@PathVariable Long combinedScoreId) throws IOException {
    return billetService.issueCopy(combinedScoreId);
  }

  /**
   * Baixa o PDF do boleto exatamente como foi gerado e guardado no bucket (R2), sem emitir uma via
   * nova no Sicoob.
   */
  @GetMapping("/{combinedScoreId}/file")
  public ResponseEntity<byte[]> downloadStoredBillet(@PathVariable Long combinedScoreId) {
    return billetService.getStoredBilletFile(combinedScoreId);
  }

  @PostMapping("/cancel/{combinedScoreId}")
  public ResponseEntity<String> cancelBillet(@PathVariable Long combinedScoreId)
      throws IOException {
    ResponseEntity<String> response = billetService.cancelBillet(combinedScoreId);
    return ResponseEntity.status(response.getStatusCode()).body("Boleto cancelado com sucesso");
  }

  /**
   * Realiza a baixa (cancelamento) manual de um boleto direto pelo "nosso número" informado, sem
   * exigir um agrupamento (CombinedScore) local conhecido. Útil para boletos avulsos/legados cuja
   * referência local não existe ou foi perdida.
   */
  @PostMapping("/cancel-by-number")
  public ResponseEntity<String> cancelBilletByNumber(@RequestParam String nossoNumero) {
    ResponseEntity<String> response = billetService.cancelBilletByNumber(nossoNumero);
    return ResponseEntity.status(response.getStatusCode()).body("Boleto cancelado com sucesso");
  }

  @GetMapping("/{combinedScoreId}")
  public ResponseEntity<BilletResponse> getBilletCombinedScore(@PathVariable long combinedScoreId)
      throws IOException {
    return ResponseEntity.ok(billetService.getBilletByCombinedScore(combinedScoreId));
  }

  /**
   * Marca manualmente como pago um agrupamento com boleto (ex: pagamento recebido fora do Sicoob),
   * removendo-o da lista de boletos em aberto.
   */
  @PatchMapping("/mark-paid/{combinedScoreId}")
  public ResponseEntity<String> markBilletAsPaid(@PathVariable Long combinedScoreId) {
    billetService.markBilletAsPaid(combinedScoreId);
    return ResponseEntity.ok("Pagamento confirmado com sucesso.");
  }
}
