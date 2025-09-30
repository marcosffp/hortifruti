package com.hortifruti.sl.hortifruti.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.config.sicoob.SicoobHttpClient;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SicoobService {

  @Value("${sicoob.num.cliente}")
  private Integer clientNumber;

  private final SicoobHttpClient httpClient;

  /**
   * Emite um boleto através da API do Sicoob
   * 
   * @param boleto Dados do boleto a ser emitido
   * @return Resposta da API contendo os detalhes do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public JsonNode emitirBoleto(JsonNode boleto) throws IOException {
    return httpClient.post("/cobranca-bancaria/v3/boletos", boleto);
  }

  /**
   * Emite um boleto e retorna o PDF através da API do Sicoob
   * 
   * @param boleto Dados do boleto a ser emitido
   * @return Resposta da API contendo o PDF do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public ResponseEntity<byte[]> emitirBoletoComPdf(JsonNode boleto) throws IOException {
    // Faz a requisição para emitir o boleto
    JsonNode resposta = emitirBoleto(boleto);

    // Extrai o PDF do boleto em Base64
    String pdfBase64 = resposta.path("pdfBoleto").asText();
    if (pdfBase64 == null || pdfBase64.isEmpty()) {
        throw new IllegalArgumentException("PDF do boleto não encontrado na resposta.");
    }

    // Converte o PDF Base64 para bytes e prepara a resposta
    return criarRespostaPdf(pdfBase64, "boleto.pdf");
  }

  /**
   * Lista os boletos de um pagador específico
   * 
   * @param numeroCpfCnpj Número do CPF ou CNPJ do pagador
   * @return Lista de boletos do pagador
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public JsonNode listarBoletosPorPagador(String numeroCpfCnpj) throws IOException {
    String endpoint = String.format(
        "/cobranca-bancaria/v3/pagadores/%s/boletos?numeroCliente=%d&codigoSituacao=1", 
        numeroCpfCnpj, 
        clientNumber
    );
    
    return httpClient.get(endpoint);
  }

  /**
   * Emite a segunda via de um boleto e retorna o PDF através da API do Sicoob
   * 
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @return Resposta da API contendo o PDF do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public ResponseEntity<byte[]> emitirSegundaViaBoleto(String nossoNumero) throws IOException {
    // Monta o endpoint com os parâmetros fixos e o nosso número
    String endpoint = String.format(
        "/cobranca-bancaria/v3/boletos/segunda-via?numeroCliente=%d&codigoModalidade=%d&nossoNumero=%s&gerarPdf=true",
        clientNumber, 1, nossoNumero);

    // Faz a requisição GET para obter a segunda via do boleto
    JsonNode resposta = httpClient.get(endpoint);
    
    // Acessa o campo "resultado"
    JsonNode resultado = resposta.path("resultado");

    // Verifica se o campo pdfBoleto está presente e não vazio
    if (!resultado.has("pdfBoleto") || resultado.get("pdfBoleto").isNull() 
            || resultado.get("pdfBoleto").asText().trim().isEmpty()) {
        throw new IllegalArgumentException("PDF do boleto não encontrado na resposta.");
    }

    // Extrai o PDF do boleto em Base64
    String pdfBase64 = resultado.get("pdfBoleto").asText();
    
    // Converte o PDF Base64 para bytes e prepara a resposta
    return criarRespostaPdf(pdfBase64, "segunda-via-boleto.pdf");
  }

  /**
   * Comanda a baixa de um boleto através da API do Sicoob
   * 
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @return Resposta da API indicando o sucesso ou falha da operação
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public ResponseEntity<String> baixarBoleto(String nossoNumero) throws IOException {
    // Monta o corpo da requisição
    Map<String, Object> boletoBaixa = new HashMap<>();
    boletoBaixa.put("numeroCliente", clientNumber);
    boletoBaixa.put("codigoModalidade", 1);

    // Monta o endpoint com o número do boleto
    String endpoint = String.format("/cobranca-bancaria/v3/boletos/%s/baixar", nossoNumero);

    // Faz a requisição POST para comandar a baixa do boleto
    return httpClient.put(endpoint, boletoBaixa);
  }

  /**
   * Atualiza os dados de um boleto já registrado
   * 
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @param boleto Dados do boleto a serem atualizados
   * @return Resposta da API indicando o sucesso ou falha da operação
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public ResponseEntity<String> atualizarBoleto(String nossoNumero, JsonNode boleto) throws IOException {
    // Monta o endpoint com o número do boleto
    String endpoint = String.format("/cobranca-bancaria/v3/boletos/%s", nossoNumero);

    // Faz a requisição PUT para atualizar o boleto
    return httpClient.put(endpoint, boleto);
  }
  
  /**
   * Cria uma resposta HTTP contendo um PDF
   * 
   * @param pdfBase64 String Base64 contendo o PDF
   * @param nomeArquivo Nome do arquivo PDF para download
   * @return Resposta HTTP com o PDF
   */
  private ResponseEntity<byte[]> criarRespostaPdf(String pdfBase64, String nomeArquivo) {
    // Decodifica o Base64 para bytes
    byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);

    // Configura os headers para retornar o PDF
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", nomeArquivo);

    // Retorna o PDF como resposta
    return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
  }
}