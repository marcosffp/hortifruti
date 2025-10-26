package com.hortifruti.sl.hortifruti.service.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.billet.BilletHttpClient;
import com.hortifruti.sl.hortifruti.dto.billet.BilletRequest;
import com.hortifruti.sl.hortifruti.dto.billet.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
@RequiredArgsConstructor
public class BilletIssue {
  private final BilletFactory billetFactory;
  private final PdfCreate pdfCreate;
  private final BilletConstants billetConstants;
  private final BilletHttpClient httpClient;
  private final BilletValidation billetValidation;
  private final BilletInfoCombinedAndClient billetInfoCombinedAndClient;

  /**
   * Emite um boleto através da API do Sicoob e retorna o PDF para download.
   *
   * @param boleto Dados simplificados do boleto
   * @return Resposta HTTP contendo o PDF do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  protected ResponseEntity<Map<String, Object>> issueBillet(BilletRequestSimplified boleto)
      throws IOException {
    try {
      JsonNode boletoJson = createBilletJson(boleto);

      JsonNode resposta = httpClient.post(billetConstants.getBASE_URL() + "boletos", boletoJson);

      JsonNode resultado = responseApi(resposta);

      Map<String, Object> responseMap = createResponseMap(resultado);

      return ResponseEntity.ok(responseMap);

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
   * Emite a segunda via de um boleto e retorna o PDF através da API do Sicoob.
   *
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @return Resposta da API contendo o PDF do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  protected ResponseEntity<byte[]> issueCopy(Long idCombinedScore) throws IOException {
    CombinedScore combinedScore =
        billetInfoCombinedAndClient.findCombinedScoreById(idCombinedScore);
    billetValidation.validateHasBillet(combinedScore);
    String nossoNumero = combinedScore.getOurNumber_sicoob();
    String endpoint = buildEndpointIssueCopy(nossoNumero);

    try {
      JsonNode resposta = httpClient.get(endpoint);

      if (resposta == null || resposta.isEmpty()) {
        throw new BilletException("A resposta da API está vazia ou nula.");
      }

      JsonNode resultado = responseApi(resposta);

      String pdfBase64 = resultado.get("pdfBoleto").asText();

      return pdfCreate.createResponsePdf(pdfBase64, "SEGUNDA-VIA-BOL-" + nossoNumero + ".pdf");

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

  private JsonNode createBilletJson(BilletRequestSimplified boleto) {
    BilletRequest boletoCompleto = billetFactory.createCompleteBoletoRequest(boleto);
    ObjectMapper mapper = new ObjectMapper();
    return mapper.valueToTree(boletoCompleto);
  }

  private JsonNode responseApi(JsonNode resposta) {
    if (resposta == null || resposta.isEmpty()) {
      throw new BilletException("A resposta da API está vazia ou nula.");
    }

    JsonNode resultado = resposta.path("resultado");
    if (!resultado.has("pdfBoleto")
        || resultado.get("pdfBoleto").isNull()
        || resultado.get("pdfBoleto").asText().trim().isEmpty()) {
      throw new BilletException("PDF do boleto não encontrado na resposta.");
    }

    return resultado;
  }

  private Map<String, Object> createResponseMap(JsonNode resultado) {
    String pdfBase64 = resultado.get("pdfBoleto").asText();
    String nossoNumero = resultado.path("nossoNumero").asText();
    String seuNumero = resultado.path("seuNumero").asText();

    byte[] pdfBytes = pdfCreate.convertBase64ToBytes(pdfBase64);

    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("pdf", pdfBytes);
    responseMap.put("nossoNumero", nossoNumero);
    responseMap.put("seuNumero", seuNumero);

    return responseMap;
  }

  private String buildEndpointIssueCopy(String nossoNumero) {
    return String.format(
        billetConstants.getBASE_URL()
            + "boletos/segunda-via?numeroCliente=%d&codigoModalidade=%d&nossoNumero=%s&gerarPdf=true",
        billetConstants.getClientNumber(),
        billetConstants.getMODALITY_CODE(),
        nossoNumero);
  }
}
