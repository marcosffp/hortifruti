package com.hortifruti.sl.hortifruti.service.billet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class BilletValidation {

  protected void validateHasBillet(CombinedScore combinedScore) {
    if (!combinedScore.isHasBillet()) {
      throw new CombinedScoreException("Agrupamento não possui boleto associado.");
    }
  }

  protected void validateResponse(ResponseEntity<JsonNode> response) {
    if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getBody() == null
        || response.getBody().isEmpty()) {
      throw new BilletException("Resposta da API está vazia ou inválida.");
    }
  }

}
