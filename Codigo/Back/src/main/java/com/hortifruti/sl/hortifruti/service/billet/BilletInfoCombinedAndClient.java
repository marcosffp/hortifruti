package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import com.hortifruti.sl.hortifruti.exception.purchase.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BilletInfoCombinedAndClient {
  private final CombinedScoreRepository combinedScoreRepository;
  private final ClientRepository clientRepository;

  public CombinedScore findCombinedScoreById(Long idCombinedScore) {
    return combinedScoreRepository
        .findById(idCombinedScore)
        .orElseThrow(
            () ->
                new CombinedScoreException(
                    "Agrupamento com o ID " + idCombinedScore + " não encontrado."));
  }

  public Client findClientById(Long clientId) {
    return clientRepository
        .findById(clientId)
        .orElseThrow(() -> new BilletException("Cliente com ID " + clientId + " não encontrado."));
  }
}
