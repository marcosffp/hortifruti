package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.sicoob.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.sicoob.BilletResponse;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.service.BilletService;
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
  @PostMapping("/issue")
  public ResponseEntity<byte[]> issueBillet(@RequestBody BilletRequestSimplified boleto) {
    try {
      return billetService.issueBillet(boleto);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest()
          .body(("Erro ao emitir boleto: " + e.getMessage()).getBytes());
    }
  }

  /**
   * Lista boletos de um pagador específico.
   *
   * @param cnpjNumber Número do CPF ou CNPJ do pagador.
   * @return Lista de boletos do pagador.
   */
  @GetMapping("/billets/{cnpjNumber}")
  public ResponseEntity<List<BilletResponse>> listBilletByPayer(@PathVariable String cnpjNumber) {
    try {
      List<BilletResponse> billets = billetService.listBilletByPayer(cnpjNumber);
      return ResponseEntity.ok(billets);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(List.of());
    }
  }

  /**
   * Emite a segunda via de um boleto e retorna o PDF.
   *
   * @param ourNumber Número identificador do boleto no Sisbr.
   * @param yourNumber Número identificador do boleto no sistema do cliente.
   * @return PDF do boleto emitido.
   */
  @GetMapping("/issue-copy/{ourNumber}/{yourNumber}")
  public ResponseEntity<byte[]> issueCopy(
      @PathVariable String ourNumber, @PathVariable String yourNumber) {
    try {
      return billetService.issueCopy(ourNumber, yourNumber);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(null);
    }
  }

  /**
   * Realiza a baixa (cancelamento) de um boleto.
   *
   * @param ourNumber Número identificador do boleto no Sisbr.
   * @return Resposta indicando o sucesso ou falha da operação.
   */
  @PostMapping("/cancel/{ourNumber}")
  public ResponseEntity<String> cancelBillet(@PathVariable String ourNumber) {
    try {
      ResponseEntity<String> response = billetService.cancelBillet(ourNumber);
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
}
