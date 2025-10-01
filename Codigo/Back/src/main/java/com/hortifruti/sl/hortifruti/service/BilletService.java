package com.hortifruti.sl.hortifruti.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.billet.BilletHttpClient;
import com.hortifruti.sl.hortifruti.dto.sicoob.BilletRequest;
import com.hortifruti.sl.hortifruti.dto.sicoob.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.sicoob.BilletResponse;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
public class BilletService {

  @Value("${sicoob.num.cliente}")
  private Integer clientNumber;

  @Value("${sicoob.num.conta.corrente}")
  private Integer accountNumber;

  private Integer MODALITY_CODE = 1;
  private String DOCUMENT_SPECIES_CODE = "DM";
  private Integer BOLETO_ISSUANCE_IDENTIFICATION = 1;
  private Integer BOLETO_DISTRIBUTION_IDENTIFICATION = 1;
  private Integer DISCOUNT_TYPE = 0;
  private Integer FINE_TYPE = 0;
  private Integer INTEREST_TYPE = 3;
  private Integer INSTALLMENT_NUMBER = 1;
  private Boolean GENERATE_PDF = true;
  private String BASE_URL = "/cobranca-bancaria/v3/";
  private final BilletHttpClient httpClient;

  private BilletRequest createCompleteBoletoRequest(BilletRequestSimplified boletoSimplificado) {
    return new BilletRequest(
        clientNumber, // numeroCliente
        MODALITY_CODE, // codigoModalidade
        accountNumber, // numeroContaCorrente
        DOCUMENT_SPECIES_CODE, // codigoEspecieDocumento
        boletoSimplificado.dataEmissao(),
        boletoSimplificado.seuNumero(),
        BOLETO_ISSUANCE_IDENTIFICATION, // identificacaoEmissaoBoleto
        BOLETO_DISTRIBUTION_IDENTIFICATION, // identificacaoDistribuicaoBoleto
        boletoSimplificado.valor(),
        boletoSimplificado.dataVencimento(),
        DISCOUNT_TYPE, // tipoDesconto
        FINE_TYPE, // tipoMulta
        INTEREST_TYPE, // tipoJurosMora
        INSTALLMENT_NUMBER, // numeroParcela
        boletoSimplificado.pagador(),
        GENERATE_PDF // gerarPdf
        );
  }

  /**
   * Emite um boleto através da API do Sicoob e retorna o PDF para download.
   *
   * @param boleto Dados simplificados do boleto
   * @return Resposta HTTP contendo o PDF do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public ResponseEntity<byte[]> issueBillet(BilletRequestSimplified boleto) throws IOException {
    try {
      // Cria o objeto BoletoRequest completo
      BilletRequest boletoCompleto = createCompleteBoletoRequest(boleto);
      ObjectMapper mapper = new ObjectMapper();
      JsonNode boletoJson = mapper.valueToTree(boletoCompleto);

      // Faz a requisição para emitir o boleto
      JsonNode resposta = httpClient.post(BASE_URL + "boletos", boletoJson);

      // Verifica se a resposta é nula ou vazia
      if (resposta == null || resposta.isEmpty()) {
        throw new BilletException("A resposta da API está vazia ou nula.");
      }

      // Acessa o campo "resultado"
      JsonNode resultado = resposta.path("resultado");

      // Verifica se o campo pdfBoleto está presente e não vazio
      if (!resultado.has("pdfBoleto")
          || resultado.get("pdfBoleto").isNull()
          || resultado.get("pdfBoleto").asText().trim().isEmpty()) {
        throw new BilletException("PDF do boleto não encontrado na resposta.");
      }

      // Extrai o PDF do boleto em Base64
      String pdfBase64 = resultado.get("pdfBoleto").asText();

      // Converte o PDF Base64 para bytes e prepara a resposta
      return createResponsePdf(pdfBase64, "BOL-" + boletoCompleto.seuNumero() + ".pdf");

    } catch (HttpClientErrorException e) {
      throw new BilletException(
          "Erro na requisição para emitir o boleto: " + e.getResponseBodyAsString(), e);
    } catch (IOException e) {
      throw new BilletException("Erro ao processar a resposta da API ao emitir o boleto.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao emitir o boleto.", e);
    }
  }

  /**
   * Lista os boletos de um pagador específico.
   *
   * @param numeroCpfCnpj Número do CPF ou CNPJ do pagador
   * @return Lista de boletos do pagador
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public List<BilletResponse> listBilletByPayer(String numeroCpfCnpj) throws IOException {
    try {
      // Monta o endpoint para a requisição
      String endpoint =
          String.format(
              BASE_URL + "pagadores/%s/boletos?numeroCliente=%d&codigoSituacao=1",
              numeroCpfCnpj,
              clientNumber);

      // Faz a requisição para obter os boletos
      JsonNode resposta = httpClient.get(endpoint);

      // Verifica se a resposta é nula
      if (resposta == null) {
        throw new BilletException("A resposta da API está nula.");
      }

      // Acessa o campo "resultado" que contém a lista de boletos
      JsonNode resultado = resposta.path("resultado");

      // Verifica se o resultado é uma lista válida
      if (!resultado.isArray()) {
        throw new BilletException("Resposta inválida da API: campo 'resultado' não é uma lista.");
      }

      // Mapeia os dados para uma lista de BoletoResponse
      List<BilletResponse> boletos = new ArrayList<>();
      for (JsonNode boletoNode : resultado) {
        BilletResponse boleto =
            new BilletResponse(
                boletoNode.path("pagador").path("nome").asText(),
                boletoNode.path("dataEmissao").asText(),
                boletoNode.path("dataVencimento").asText(),
                boletoNode.path("seuNumero").asText(),
                boletoNode.path("situacaoBoleto").asText(),
                boletoNode.path("nossoNumero").asText(),
                boletoNode.path("valor").decimalValue());
        boletos.add(boleto);
      }

      return boletos;

    } catch (HttpClientErrorException e) {
      throw new BilletException(
          "Erro na requisição para listar boletos: " + e.getResponseBodyAsString(), e);
    } catch (IOException e) {
      throw new BilletException("Erro ao processar a resposta da API ao listar boletos.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao listar boletos.", e);
    }
  }

  /**
   * Emite a segunda via de um boleto e retorna o PDF através da API do Sicoob.
   *
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @return Resposta da API contendo o PDF do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public ResponseEntity<byte[]> issueCopy(String nossoNumero, String seuNumero) throws IOException {
    // Monta o endpoint para a requisição
    String endpoint =
        String.format(
            BASE_URL
                + "boletos/segunda-via?numeroCliente=%d&codigoModalidade=%d&nossoNumero=%s&gerarPdf=true",
            clientNumber,
            MODALITY_CODE,
            nossoNumero);

    try {
      // Faz a requisição GET para a API
      JsonNode resposta = httpClient.get(endpoint);

      // Verifica se a resposta é nula ou vazia
      if (resposta == null || resposta.isEmpty()) {
        throw new BilletException("A resposta da API está vazia ou nula.");
      }

      // Acessa o campo "resultado"
      JsonNode resultado = resposta.path("resultado");

      // Verifica se o campo "pdfBoleto" está presente e não vazio
      if (!resultado.has("pdfBoleto")
          || resultado.get("pdfBoleto").isNull()
          || resultado.get("pdfBoleto").asText().trim().isEmpty()) {
        throw new BilletException("PDF do boleto não encontrado na resposta.");
      }

      // Extrai o PDF do boleto em Base64
      String pdfBase64 = resultado.get("pdfBoleto").asText();

      // Converte o PDF Base64 para bytes e prepara a resposta
      return createResponsePdf(pdfBase64, "SEGUNDA-VIA-BOL-" + seuNumero + ".pdf");

    } catch (HttpClientErrorException.NotFound e) {
      throw new BilletException(
          "Boleto não encontrado. Verifique o 'nossoNumero' e tente novamente.", e);
    } catch (IOException e) {
      throw new BilletException(
          "Erro ao processar a resposta da API ao emitir a segunda via do boleto.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao emitir a segunda via do boleto.", e);
    }
  }

  /**
   * Realiza a baixa (cancelamento) de um boleto através da API do Sicoob.
   *
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @return Resposta indicando o sucesso ou falha da operação
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   * @throws BilletException Se houver erro específico da API de boletos
   */
  public ResponseEntity<String> cancelBillet(String nossoNumero)
      throws IOException, BilletException {
    try {
      // Monta o objeto de requisição para a baixa do boleto
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("numeroCliente", clientNumber);
      requestBody.put("codigoModalidade", MODALITY_CODE);

      // Monta o endpoint com o número do boleto
      String endpoint = String.format(BASE_URL + "boletos/%s/baixar", nossoNumero);

      // Faz a requisição POST para realizar a baixa do boleto
      httpClient.post(endpoint, requestBody);

      // Se chegou até aqui, a operação foi bem-sucedida (código 204)
      return ResponseEntity.noContent().build();

    } catch (HttpClientErrorException.BadRequest e) {
      // Extrai a mensagem detalhada de erro para códigos 400
      String errorBody = e.getResponseBodyAsString();

      // Se o erro contém "Título em processo de baixa/liquidação", trata como sucesso
      if (errorBody.contains("Título em processo de baixa/liquidação")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("O boleto já está em processo de cancelamento ou já foi liquidado.");
      }

      throw new BilletException("Erro na requisição para baixar o boleto: " + errorBody);
    } catch (HttpClientErrorException e) {
      // Para outros erros HTTP do cliente
      throw new BilletException(
          "Erro na requisição para baixar o boleto: " + e.getResponseBodyAsString());
    } catch (IOException e) {
      throw new BilletException("Erro de comunicação ao baixar o boleto: " + e.getMessage());
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao baixar o boleto: " + e.getMessage());
    }
  }

  /**
   * Cria uma resposta HTTP contendo um PDF.
   *
   * @param pdfBase64 String Base64 contendo o PDF
   * @param nomeArquivo Nome do arquivo PDF para download
   * @return Resposta HTTP com o PDF
   * @throws BilletException Se o PDF em Base64 for inválido ou ocorrer algum erro na decodificação
   */
  private ResponseEntity<byte[]> createResponsePdf(String pdfBase64, String nomeArquivo) {
    try {
      // Verifica se o Base64 está vazio ou nulo
      if (pdfBase64 == null || pdfBase64.trim().isEmpty()) {
        throw new BilletException("O conteúdo do PDF em Base64 está vazio ou nulo.");
      }

      // Decodifica o Base64 para bytes
      byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);

      // Configura os headers para retornar o PDF
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("attachment", nomeArquivo);

      // Retorna o PDF como resposta
      return ResponseEntity.ok().headers(headers).body(pdfBytes);

    } catch (IllegalArgumentException e) {
      throw new BilletException(
          "Erro ao decodificar o PDF em Base64. O conteúdo pode estar corrompido ou inválido.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao criar a resposta do PDF.", e);
    }
  }
}
