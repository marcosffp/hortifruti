package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.dto.billet.OpenBilletResponse;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billet")
@AllArgsConstructor
public class BilletController {

  private final BilletService billetService;

  /**
   * Emite um boleto e retorna o PDF.
   *
   * @param combinedScoreId ID do CombinedScore
   * @param number Número identificador do cliente
   * @param dueDate Data de vencimento opcional (formato yyyy-MM-dd)
   * @return PDF do boleto emitido.
   */
  @GetMapping("/generate/{combinedScoreId}")
  public ResponseEntity<byte[]> generateBillet(
      @PathVariable Long combinedScoreId,
      @RequestParam String number,
      @RequestParam(required = false) String dueDate)
      throws IOException {
    try {
      return billetService.generateBillet(combinedScoreId, number, dueDate);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest()
          .body(("Erro ao emitir boleto: " + e.getMessage()).getBytes());
    }
  }

  /**
   * Lista boletos de um pagador específico. Sem filtros, retorna os boletos em aberto.
   *
   * @param clientId ID do cliente (CPF ou CNPJ).
   * @param codigoSituacao Situação do boleto (1=Em aberto, 2=Baixado, 3=Liquidado), opcional.
   * @param dataInicio Data de vencimento inicial do filtro, opcional.
   * @param dataFim Data de vencimento final do filtro, opcional.
   * @return Lista de boletos do pagador que atendem aos filtros informados.
   */
  @GetMapping("/client/{clientId}")
  public ResponseEntity<List<BilletResponse>> listBilletByPayer(
      @PathVariable long clientId,
      @RequestParam(required = false) Integer codigoSituacao,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim) {
    try {
      boolean hasFilters = codigoSituacao != null || dataInicio != null || dataFim != null;
      List<BilletResponse> billets =
          hasFilters
              ? billetService.listBilletByPayer(clientId, codigoSituacao, dataInicio, dataFim)
              : billetService.listBilletByPayer(clientId);
      return ResponseEntity.ok(billets);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(List.of());
    }
  }

  /**
   * Lista todos os boletos em aberto de todos os clientes, ordenados por data de vencimento.
   *
   * @return Lista de boletos em aberto.
   */
  @GetMapping("/open")
  public ResponseEntity<List<OpenBilletResponse>> listAllOpenBillets() {
    try {
      return ResponseEntity.ok(billetService.listAllOpenBillets());
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
    }
  }

  /**
   * Emite a segunda via de um boleto e retorna o PDF.
   *
   * @param idCombinedScore ID do CombinedScore associado ao boleto.
   * @return PDF do boleto emitido.
   */
  @GetMapping("/issue-copy/{idCombinedScore}")
  public ResponseEntity<byte[]> issueCopy(@PathVariable Long idCombinedScore) {
    try {
      return billetService.issueCopy(idCombinedScore);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(null);
    }
  }

  /**
   * Realiza a baixa (cancelamento) de um boleto.
   *
   * @param idCombinedScore ID do CombinedScore associado ao boleto.
   * @return Resposta indicando o sucesso ou falha da operação.
   */
  @PostMapping("/cancel/{idCombinedScore}")
  public ResponseEntity<String> cancelBillet(@PathVariable Long idCombinedScore) {
    try {
      ResponseEntity<String> response = billetService.cancelBillet(idCombinedScore);
      return ResponseEntity.status(response.getStatusCode()).body("Boleto cancelado com sucesso");
    } catch (BilletException e) {
      e.printStackTrace();

      String errorMessage = e.getMessage();
      if (errorMessage.contains("Título em processo de baixa/liquidação")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("O boleto já está em processo de cancelamento ou já foi liquidado");
      }

      if (errorMessage.contains("Erro na requisição")) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Erro ao cancelar boleto: " + extractErrorMessage(errorMessage));
      }

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erro ao processar cancelamento: " + e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erro inesperado ao cancelar boleto: " + e.getMessage());
    }
  }

  /** Extrai a mensagem de erro principal do JSON de erro retornado pela API */
  private String extractErrorMessage(String errorJson) {
    try {
      if (errorJson != null && errorJson.contains("mensagem")) {
        int start = errorJson.indexOf("\"mensagem\":\"") + 12;
        int end = errorJson.indexOf("\"", start);
        if (start > 12 && end > start) {
          return errorJson.substring(start, end);
        }
      }
      return errorJson;
    } catch (Exception e) {
      return errorJson;
    }
  }

  /**
   * Lista o boleto associado a um CombinedScore específico.
   *
   * @param combinedScoreId ID do CombinedScore
   * @return Detalhes do boleto associado
   */
  @GetMapping("/{combinedScoreId}")
  public ResponseEntity<BilletResponse> getBilletCombinedScore(@PathVariable long combinedScoreId) {
    try {
      BilletResponse billet = billetService.getBilletByCombinedScore(combinedScoreId);
      return ResponseEntity.ok(billet);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  /**
   * Marca manualmente como pago um agrupamento com boleto (ex: pagamento recebido fora do
   * Sicoob), removendo-o da lista de boletos em aberto.
   *
   * @param combinedScoreId ID do CombinedScore associado ao boleto.
   * @return Resposta indicando o sucesso ou falha da operação.
   */
  @PatchMapping("/mark-paid/{combinedScoreId}")
  public ResponseEntity<String> markBilletAsPaid(@PathVariable Long combinedScoreId) {
    try {
      billetService.markBilletAsPaid(combinedScoreId);
      return ResponseEntity.ok("Pagamento confirmado com sucesso.");
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body("Erro ao confirmar pagamento: " + e.getMessage());
    }
  }
}
