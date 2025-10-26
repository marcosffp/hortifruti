package com.hortifruti.sl.hortifruti.service.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.config.billet.BilletHttpClient;
import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
@AllArgsConstructor
public class BilletQuery {

  private final BilletConstants billetConstants;
  private final BilletHttpClient httpClient;
  private final BilletValidation billetValidation;
  private final BilletInfoCombinedAndClient billetInfoCombinedAndClient;

  /**
   * Lista os boletos de um pagador específico.
   *
   * @param clientId ID do cliente (CPF ou CNPJ)
   * @return Lista de boletos do pagador
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public List<BilletResponse> listBilletByPayer(long clientId) throws IOException {
    String numeroCpfCnpj = getClientDocument(clientId);
    String endpoint = buildListBilletEndpoint(numeroCpfCnpj);
    try {
      ResponseEntity<JsonNode> response = httpClient.getWithResponse(endpoint);
      billetValidation.validateResponse(response);
      JsonNode resultado = getResponseResult(response);
      List<BilletResponse> boletos = new ArrayList<>();
      for (JsonNode boletoNode : resultado) {
        BilletResponse boleto = mapJsonToBilletResponse(boletoNode);
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
   * Consulta o boleto específico associado a um CombinedScore.
   *
   * @param combinedScoreId ID do CombinedScore
   * @return Detalhes do boleto associado
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public BilletResponse getBilletByCombinedScore(long combinedScoreId) throws IOException {
    CombinedScore combinedScore =
        billetInfoCombinedAndClient.findCombinedScoreById(combinedScoreId);
    billetValidation.validateHasBillet(combinedScore);

    try {
      String endpoint = buildBilletEndpoint(combinedScore.getOurNumber_sicoob());

      // Faz a requisição para obter o boleto
      ResponseEntity<JsonNode> response = httpClient.getWithResponse(endpoint);
      billetValidation.validateResponse(response);

      // Verifica se a resposta é válida
      JsonNode resposta = response.getBody();
      if (resposta == null || resposta.isEmpty()) {
        throw new BilletException(
            "Nenhum boleto encontrado para o número: " + combinedScore.getOurNumber_sicoob());
      }

      // Acessa o campo "resultado" que contém os detalhes do boleto
      JsonNode resultado = getResponseResult(response);

      // Mapeia os dados para um objeto BilletResponse
      return mapJsonToBilletResponse(resultado);

    } catch (HttpClientErrorException e) {
      throw new BilletException(
          "Erro na requisição para buscar o boleto: " + e.getResponseBodyAsString(), e);
    } catch (IOException e) {
      throw new BilletException("Erro ao processar a resposta da API ao buscar o boleto.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao buscar o boleto.", e);
    }
  }

  private JsonNode getResponseResult(ResponseEntity<JsonNode> response) {
    JsonNode resultado = response.getBody().path("resultado");

    return resultado;
  }

  private String buildListBilletEndpoint(String numeroCpfCnpj) {
    return String.format(
        billetConstants.getBASE_URL() + "pagadores/%s/boletos?numeroCliente=%d&codigoSituacao=1",
        numeroCpfCnpj,
        billetConstants.getClientNumber());
  }

  private String getClientDocument(long clientId) {
    return billetInfoCombinedAndClient
        .findClientById(clientId)
        .getDocument()
        .replaceAll("[^\\d]", "");
  }

  private BilletResponse mapJsonToBilletResponse(JsonNode boletoNode) {
    return new BilletResponse(
        boletoNode.path("pagador").path("nome").asText(),
        boletoNode.path("dataEmissao").asText(),
        boletoNode.path("dataVencimento").asText(),
        boletoNode.path("seuNumero").asText(),
        boletoNode.path("situacaoBoleto").asText(),
        boletoNode.path("valor").decimalValue());
  }

  private String buildBilletEndpoint(String nossoNumero) {
    return String.format(
        billetConstants.getBASE_URL()
            + "boletos?numeroCliente=%d&codigoModalidade=%d&nossoNumero=%s",
        billetConstants.getClientNumber(),
        billetConstants.getMODALITY_CODE(),
        nossoNumero);
  }
}
