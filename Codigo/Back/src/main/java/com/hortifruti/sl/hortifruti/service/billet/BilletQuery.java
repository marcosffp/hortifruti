package com.hortifruti.sl.hortifruti.service.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.config.billet.BilletHttpClient;
import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
@AllArgsConstructor
public class BilletQuery {

  private static final Integer SITUACAO_EM_ABERTO = 1;

  private final BilletConstants billetConstants;
  private final BilletHttpClient httpClient;
  private final BilletValidation billetValidation;
  private final BilletInfoCombinedAndClient billetInfoCombinedAndClient;
  private final CombinedScoreRepository combinedScoreRepository;

  /**
   * Lista os boletos em aberto de um pagador específico.
   *
   * @param clientId ID do cliente (CPF ou CNPJ)
   * @return Lista de boletos em aberto do pagador
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public List<BilletResponse> listBilletByPayer(long clientId) throws IOException {
    return listBilletByPayer(clientId, SITUACAO_EM_ABERTO, null, null);
  }

  /**
   * Lista os boletos de um pagador específico, com filtros opcionais de situação e período de
   * vencimento.
   *
   * @param clientId ID do cliente (CPF ou CNPJ)
   * @param codigoSituacao Situação do boleto (1=Em aberto, 2=Baixado, 3=Liquidado), opcional
   * @param dataInicio Data de vencimento inicial do filtro, opcional
   * @param dataFim Data de vencimento final do filtro, opcional
   * @return Lista de boletos do pagador que atendem aos filtros informados
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public List<BilletResponse> listBilletByPayer(
      long clientId, Integer codigoSituacao, LocalDate dataInicio, LocalDate dataFim)
      throws IOException {
    String numeroCpfCnpj = getClientDocument(clientId);
    String endpoint = buildListBilletEndpoint(numeroCpfCnpj, codigoSituacao, dataInicio, dataFim);
    try {
      ResponseEntity<JsonNode> response = httpClient.getWithResponse(endpoint);
      if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
        return List.of();
      }
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

    return getBilletByOurNumber(combinedScore.getOurNumber_sicoob());
  }

  /**
   * Consulta no Sicoob a situação atual de um único boleto pelo seu "nossoNumero". Diferente de
   * {@link #listBilletByPayer}, não depende de codigoSituacao/dataInicio/dataFim — por isso é a
   * forma confiável de confirmar a situação de um boleto específico, mesmo que ele esteja fora da
   * janela padrão retornada pela listagem (ex: boletos muito vencidos).
   *
   * @param ourNumberSicoob "nossoNumero" do boleto no Sicoob
   * @return Detalhes do boleto
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public BilletResponse getBilletByOurNumber(String ourNumberSicoob) throws IOException {
    String endpoint = buildBilletEndpoint(ourNumberSicoob);

    // httpClient.getWithResponse já converte erros HTTP em BilletException com o detalhe da
    // resposta do Sicoob — não deve ser re-envolvida aqui, para não perder essa informação.
    ResponseEntity<JsonNode> response = httpClient.getWithResponse(endpoint);
    billetValidation.validateResponse(response);

    JsonNode resultado = getResponseResult(response);
    if (resultado == null || resultado.isMissingNode() || resultado.isEmpty()) {
      throw new BilletException("Nenhum boleto encontrado para o número: " + ourNumberSicoob);
    }

    return mapJsonToBilletResponse(resultado);
  }

  private JsonNode getResponseResult(ResponseEntity<JsonNode> response) {
    JsonNode resultado = response.getBody().path("resultado");

    return resultado;
  }

  private String buildListBilletEndpoint(
      String numeroCpfCnpj, Integer codigoSituacao, LocalDate dataInicio, LocalDate dataFim) {
    StringBuilder endpoint =
        new StringBuilder(billetConstants.getBASE_URL())
            .append("pagadores/")
            .append(numeroCpfCnpj)
            .append("/boletos?numeroCliente=")
            .append(billetConstants.getClientNumber());

    if (codigoSituacao != null) {
      endpoint.append("&codigoSituacao=").append(codigoSituacao);
    }
    if (dataInicio != null) {
      endpoint.append("&dataInicio=").append(dataInicio);
    }
    if (dataFim != null) {
      endpoint.append("&dataFim=").append(dataFim);
    }

    return endpoint.toString();
  }

  private String getClientDocument(long clientId) {
    return billetInfoCombinedAndClient
        .findClientById(clientId)
        .getDocument()
        .replaceAll("[^\\d]", "");
  }

  private BilletResponse mapJsonToBilletResponse(JsonNode boletoNode) {
    String seuNumero = boletoNode.path("seuNumero").asText();
    Long combinedScoreId =
        combinedScoreRepository.findAllByYourNumber(seuNumero).stream()
            .max(Comparator.comparing(CombinedScore::getId))
            .map(CombinedScore::getId)
            .orElse(null);

    return new BilletResponse(
        boletoNode.path("pagador").path("nome").asText(),
        boletoNode.path("dataEmissao").asText(),
        boletoNode.path("dataVencimento").asText(),
        seuNumero,
        boletoNode.path("situacaoBoleto").asText(),
        boletoNode.path("valor").decimalValue(),
        combinedScoreId);
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
