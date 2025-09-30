package com.hortifruti.sl.hortifruti.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.service.SicoobService;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sicoob-test")
@AllArgsConstructor
public class SicoobTestController {

  private final SicoobService sicoobService;



  /**
   * Emite um boleto e retorna o PDF
   * 
   * @param boleto Dados do boleto a ser emitido
   * @return PDF do boleto emitido
   */
  @PostMapping("/emitir-boleto")
  public ResponseEntity<byte[]> emitirBoleto(@RequestBody JsonNode boleto) {
    try {
      return sicoobService.emitirBoletoComPdf(boleto);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(null);
    }
  }

  /**
   * Lista boletos de um pagador específico
   * 
   * @param numeroCpfCnpj Número do CPF ou CNPJ do pagador
   * @return Lista de boletos do pagador
   */
  @GetMapping("/pagadores/{numeroCpfCnpj}/boletos")
  public ResponseEntity<?> listarBoletosPorPagador(@PathVariable String numeroCpfCnpj) {
    try {
      JsonNode boletos = sicoobService.listarBoletosPorPagador(numeroCpfCnpj);
      return ResponseEntity.ok(boletos);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body("Erro ao listar boletos: " + e.getMessage());
    }
  }


  /**
   * Emite a segunda via de um boleto e retorna o PDF
   * 
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @return PDF do boleto emitido
   */
  @GetMapping("/segunda-via-boleto/{nossoNumero}")
  public ResponseEntity<byte[]> emitirSegundaViaBoleto(@PathVariable String nossoNumero) {
    try {
      return sicoobService.emitirSegundaViaBoleto(nossoNumero);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(null);
    }
  }

  /**
   * Comanda a baixa de um boleto
   * 
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @return Resposta da API indicando o sucesso ou falha da operação
   */
  @PostMapping("/baixar-boleto/{nossoNumero}")
  public ResponseEntity<String> baixarBoleto(@PathVariable String nossoNumero) {
    try {
      ResponseEntity<String> response = sicoobService.baixarBoleto(nossoNumero);
      return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body("Erro ao baixar boleto: " + e.getMessage());
    }
  }

  /**
   * Atualiza os dados de um boleto já registrado
   * 
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @param boleto Dados do boleto a serem atualizados
   * @return Resposta da API indicando o sucesso ou falha da operação
   */
  @PutMapping("/atualizar-boleto/{nossoNumero}")
  public ResponseEntity<String> atualizarBoleto(
      @PathVariable String nossoNumero,
      @RequestBody JsonNode boleto) {
    try {
      ResponseEntity<String> response = sicoobService.atualizarBoleto(nossoNumero, boleto);
      return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body("Erro ao atualizar boleto: " + e.getMessage());
    }
  }
}
