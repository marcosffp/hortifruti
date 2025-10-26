package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
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

  // Método auxiliar para buscar o CombinedScore pelo ID
  protected CombinedScore findCombinedScoreById(Long idCombinedScore) {
    return combinedScoreRepository
        .findById(idCombinedScore)
        .orElseThrow(
            () ->
                new CombinedScoreException(
                    "Agrupamento com o ID " + idCombinedScore + " não encontrado."));
  }

  // Método auxiliar para buscar o Client pelo ID
  protected Client findClientById(Long clientId) {
    return clientRepository
        .findById(clientId)
        .orElseThrow(() -> new BilletException("Cliente com ID " + clientId + " não encontrado."));
  }
}
