package com.hortifruti.sl.hortifruti.service.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.config.billet.BilletHttpClient;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Component
@RequiredArgsConstructor
public class BilletCancel {

  private final CombinedScoreService combinedScoreService;
  private final BilletHttpClient httpClient;
  private final BilletConstants billetConstants;
  private final BilletValidation billetValidation;
  private final BilletInfoCombinedAndClient billetInfoCombinedAndClient;

  /**
   * Realiza a baixa (cancelamento) de um boleto através da API do Sicoob.
   *
   * @param idCombinedScore ID do CombinedScore
   * @return Resposta indicando o sucesso ou falha da operação
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   * @throws BilletException Se houver erro específico da API de boletos
   */
  public ResponseEntity<String> cancelBillet(Long idCombinedScore)
      throws IOException, BilletException {
    CombinedScore combinedScore =
        billetInfoCombinedAndClient.findCombinedScoreById(idCombinedScore);
    billetValidation.validateHasBillet(combinedScore);

    try {
      String nossoNumero = combinedScore.getOurNumber_sicoob();
      String endpoint = buildCancelEndpoint(nossoNumero);
      Map<String, Object> requestBody = buildCancelRequestBody();

      JsonNode response = httpClient.postCancel(endpoint, requestBody);

      return handleCancelResponse(response, combinedScore);

    } catch (HttpClientErrorException.BadRequest e) {
      return handleBadRequest(e);
    } catch (HttpClientErrorException e) {
      throw new BilletException(
          "Erro na requisição para baixar o boleto: " + e.getResponseBodyAsString());
    } catch (HttpServerErrorException e) {
      throw new BilletException("Erro interno do servidor ao baixar o boleto: " + e.getMessage());
    } catch (IOException e) {
      throw new BilletException("Erro de comunicação ao baixar o boleto: " + e.getMessage());
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao baixar o boleto: " + e.getMessage());
    }
  }

  // Método auxiliar para construir o endpoint de cancelamento
  private String buildCancelEndpoint(String nossoNumero) {
    return String.format(billetConstants.getBASE_URL() + "boletos/%s/baixar", nossoNumero);
  }

  // Método auxiliar para construir o corpo da requisição de cancelamento
  private Map<String, Object> buildCancelRequestBody() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("numeroCliente", billetConstants.getClientNumber());
    requestBody.put("codigoModalidade", billetConstants.getMODALITY_CODE());
    return requestBody;
  }

  // Método auxiliar para tratar a resposta de cancelamento
  private ResponseEntity<String> handleCancelResponse(
      JsonNode response, CombinedScore combinedScore) {
    if (response == null) {
      combinedScoreService.updateStatusAfterBilletCancellation(combinedScore.getYourNumber());
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok("Boleto baixado com sucesso.");
  }

  // Método auxiliar para tratar erros de requisição (400 Bad Request)
  private ResponseEntity<String> handleBadRequest(HttpClientErrorException.BadRequest e) {
    String errorBody = e.getResponseBodyAsString();
    if (errorBody.contains("Título em processo de baixa/liquidação")) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("O boleto já está em processo de cancelamento ou já foi liquidado.");
    }
    throw new BilletException("Erro na requisição para baixar o boleto: " + errorBody);
  }
}
