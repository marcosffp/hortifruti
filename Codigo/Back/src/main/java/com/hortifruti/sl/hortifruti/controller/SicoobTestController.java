package com.hortifruti.sl.hortifruti.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.service.SicoobService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@RestController
@RequestMapping("/api/sicoob-test")
public class SicoobTestController {

  private final SicoobService sicoobService;

  public SicoobTestController(SicoobService sicoobService) {
    this.sicoobService = sicoobService;
  }

  @GetMapping("/token")
  public ResponseEntity<String> testTokenGeneration() {
    try {
      String token = sicoobService.getAccessToken();
      return ResponseEntity.ok("Token gerado com sucesso: " + token);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body("Erro ao gerar token: " + e.getMessage());
    }
  }

  @PostMapping("/emitir-boleto")
  public ResponseEntity<?> emitirBoleto(@RequestBody JsonNode boleto) {
    try {
      JsonNode resposta = sicoobService.emitirBoleto(boleto);
      return ResponseEntity.ok(resposta);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body("Erro ao emitir boleto: " + e.getMessage());
    }
  }

  @GetMapping("/boleto-pdf/{nossoNumero}")
  public ResponseEntity<byte[]> getBoletoPdf(@PathVariable String nossoNumero) {
    try {
      JsonNode resposta = sicoobService.get("/cobranca-bancaria/v3/boletos/" + nossoNumero);
      String pdfBase64 = resposta.path("pdfBoleto").asText();
      byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("attachment", "boleto.pdf");

      return ResponseEntity.ok()
          .headers(headers)
          .body(pdfBytes);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(null);
    }
  }

  @GetMapping("/emitir-boleto-fixo")
  public ResponseEntity<?> emitirBoletoFixo() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String json = "{"
          + "\"numeroCliente\": 581315,"
          + "\"codigoModalidade\": 1,"
          + "\"numeroContaCorrente\": 610119," // <-- CORRIGIDO!
          + "\"codigoEspecieDocumento\": \"DM\","
          + "\"dataEmissao\": \"2025-09-26\","
          + "\"seuNumero\": \"962\","
          + "\"identificacaoEmissaoBoleto\": 1,"
          + "\"identificacaoDistribuicaoBoleto\": 1,"
          + "\"valor\": 10.00,"
          + "\"dataVencimento\": \"2025-10-01\","
          + "\"tipoDesconto\": 0,"
          + "\"tipoMulta\": 0,"
          + "\"tipoJurosMora\": 3,"
          + "\"numeroParcela\": 1,"
          + "\"pagador\": {"
          + "  \"numeroCpfCnpj\": \"10297478000189\","
          + "  \"nome\": \"INDUSTRIA DE CARNES GRANDMINAS LTDA\","
          + "  \"endereco\": \"Rua Quartzolit\","
          + "  \"bairro\": \"Sítio Boa Vista\","
          + "  \"cidade\": \"Santa Luzia\","
          + "  \"cep\": \"33040257\","
          + "  \"uf\": \"MG\""
          + "},"
          + "\"gerarPdf\": true"
          + "}";
      JsonNode boleto = mapper.readTree(json);
      JsonNode resposta = sicoobService.emitirBoleto(boleto);
      return ResponseEntity.ok(resposta);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body("Erro ao emitir boleto: " + e.getMessage());
    }
  }
}
