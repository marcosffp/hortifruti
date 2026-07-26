package com.hortifruti.sl.hortifruti.service.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.config.billet.BilletHttpClient;
import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import com.hortifruti.sl.hortifruti.service.storage.BilletFileStorageService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BilletCancel {

  private final CombinedScoreService combinedScoreService;
  private final BilletHttpClient httpClient;
  private final BilletConstants billetConstants;
  private final BilletValidation billetValidation;
  private final BilletInfoCombinedAndClient billetInfoCombinedAndClient;
  private final BilletFileStorageService billetFileStorageService;

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

    } catch (BilletException e) {
      return handlePostCancelFailure(e);
    } catch (IOException e) {
      throw new BilletException("Erro de comunicação ao baixar o boleto: " + e.getMessage());
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao baixar o boleto: " + e.getMessage());
    }
  }

  /**
   * Realiza a baixa (cancelamento) de um boleto diretamente pelo "nosso número" informado pelo
   * operador, sem exigir um {@link CombinedScore} local conhecido — usado no cancelamento
   * manual/avulso (ex: boleto legado, ou cuja referência local foi perdida). Se existir um
   * agrupamento local com esse número, seu status é atualizado; caso contrário a baixa no Sicoob é
   * considerada bem-sucedida mesmo assim.
   *
   * @param nossoNumero Nosso número do boleto no Sicoob
   * @return Resposta indicando o sucesso ou falha da operação
   */
  public ResponseEntity<String> cancelBilletByNumber(String nossoNumero) throws BilletException {
    if (nossoNumero == null || nossoNumero.trim().isEmpty()) {
      throw new BilletException("Informe o nosso número do boleto para realizar a baixa.");
    }

    String trimmedNumber = nossoNumero.trim();

    try {
      String endpoint = buildCancelEndpoint(trimmedNumber);
      Map<String, Object> requestBody = buildCancelRequestBody();

      JsonNode response = httpClient.postCancel(endpoint, requestBody);

      return handleManualCancelResponse(response, trimmedNumber);

    } catch (BilletException e) {
      return handlePostCancelFailure(e);
    } catch (IOException e) {
      throw new BilletException("Erro de comunicação ao baixar o boleto: " + e.getMessage());
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao baixar o boleto: " + e.getMessage());
    }
  }

  private ResponseEntity<String> handleManualCancelResponse(JsonNode response, String nossoNumero) {
    if (response == null) {
      updateLocalRecordsBestEffort(nossoNumero);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok("Boleto baixado com sucesso.");
  }

  /**
   * Atualiza o(s) agrupamento(s) local(is) vinculado(s) a este "nosso número", se existirem. A
   * baixa manual é destinada justamente a boletos sem CombinedScore correspondente — por isso a
   * ausência de correspondência local é esperada e não deve reverter a baixa já confirmada no
   * Sicoob.
   */
  private void updateLocalRecordsBestEffort(String nossoNumero) {
    try {
      List<CombinedScore> matches = combinedScoreService.findAllByOurNumberSicoob(nossoNumero);
      if (matches.isEmpty()) {
        log.info(
            "Boleto {} baixado no Sicoob sem CombinedScore local correspondente (baixa avulsa).",
            nossoNumero);
        return;
      }
      for (CombinedScore combinedScore : matches) {
        combinedScoreService.updateStatusAfterBilletCancellation(combinedScore.getYourNumber());
        billetFileStorageService.cancelBilletFileAfterCommit(combinedScore.getId());
      }
    } catch (Exception e) {
      log.warn(
          "Boleto {} baixado no Sicoob, mas falhou ao atualizar o registro local correspondente.",
          nossoNumero,
          e);
    }
  }

  private String buildCancelEndpoint(String nossoNumero) {
    return String.format(billetConstants.getBASE_URL() + "boletos/%s/baixar", nossoNumero);
  }

  private Map<String, Object> buildCancelRequestBody() {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("numeroCliente", billetConstants.getClientNumber());
    requestBody.put("codigoModalidade", billetConstants.getMODALITY_CODE());
    return requestBody;
  }

  private ResponseEntity<String> handleCancelResponse(
      JsonNode response, CombinedScore combinedScore) {
    if (response == null) {
      combinedScoreService.updateStatusAfterBilletCancellation(combinedScore.getYourNumber());
      billetFileStorageService.cancelBilletFileAfterCommit(combinedScore.getId());
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok("Boleto baixado com sucesso.");
  }

  /**
   * {@link BilletHttpClient} já embrulha qualquer {@link HttpClientErrorException}/{@code
   * HttpServerErrorException} vindo do Sicoob em {@link BilletException} antes que o erro chegue
   * aqui — por isso o corpo da resposta original é inspecionado através de {@code getCause()}, e
   * não capturando o tipo HTTP diretamente (que nunca chegaria cru a este método).
   */
  private ResponseEntity<String> handlePostCancelFailure(BilletException e) {
    if (e.getCause() instanceof HttpClientErrorException.BadRequest badRequest) {
      String errorBody = badRequest.getResponseBodyAsString();
      if (errorBody.contains("Título em processo de baixa/liquidação")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("O boleto já está em processo de cancelamento ou já foi liquidado.");
      }
    }
    throw e;
  }
}
