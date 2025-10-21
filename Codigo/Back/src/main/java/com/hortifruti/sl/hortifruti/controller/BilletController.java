package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
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
   * @param boleto Dados do boleto a ser emitido.
   * @return PDF do boleto emitido.
   */
  @GetMapping("/generate/{combinedScoreId}")
  public ResponseEntity<byte[]> generateBillet(@PathVariable Long combinedScoreId, String number)
      throws IOException {
    try {
      return billetService.generateBillet(combinedScoreId, number);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest()
          .body(("Erro ao emitir boleto: " + e.getMessage()).getBytes());
    }
  }

  /**
   * Lista boletos de um pagador específico.
   *
   * @param clientId ID do cliente (CPF ou CNPJ).
   * @return Lista de boletos do pagador.
   */
  @GetMapping("/client/{clientId}")
  public ResponseEntity<List<BilletResponse>> listBilletByPayer(@PathVariable long clientId) {
    try {
      List<BilletResponse> billets = billetService.listBilletByPayer(clientId);
      return ResponseEntity.ok(billets);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(List.of());
    }
  }

  /**
   * Emite a segunda via de um boleto e retorna o PDF.
   *
   * @param  idCombinedScore ID do CombinedScore associado ao boleto.
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

      // Mensagem mais precisa para o erro específico
      String errorMessage = e.getMessage();
      if (errorMessage.contains("Título em processo de baixa/liquidação")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("O boleto já está em processo de cancelamento ou já foi liquidado");
      }

      // Para outros erros da API
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
        // Formato simples para extrair a primeira mensagem de erro
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
   * Lista boletos com filtros opcionais (nome do cliente, período, status) e paginação.
   *
   * @param name Nome do cliente (opcional).
   * @param startDate Data inicial do período (opcional).
   * @param endDate Data final do período (opcional).
   * @param status Status do boleto (opcional).
   * @param page Número da página (opcional, padrão: 0).
   * @param size Tamanho da página (opcional, padrão: 10).
   * @return Lista paginada de boletos.
   */
  @GetMapping("/search")
  public ResponseEntity<?> searchBillets(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "15") int size) {
    try {
      return ResponseEntity.ok(
          billetService.searchBillets(name, startDate, endDate, status, page, size));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erro ao buscar boletos: " + e.getMessage());
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
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(null); // Return a default error response
    }
  }
}
