package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
import com.hortifruti.sl.hortifruti.mapper.CombinedScoreMapper;
import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.model.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.CombinedScoreRepository;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CombinedScoreService {

  private final CombinedScoreRepository combinedScoreRepository;
  private final CombinedScoreMapper combinedScoreMapper;
  private final ClientRepository clientRepository;

  public CombinedScoreResponse confirmGrouping(CombinedScoreRequest request) {
    if (request.clientId() == null) {
      throw new CombinedScoreException("O ID do cliente é obrigatório.");
    }

    CombinedScore groupedProducts = combinedScoreMapper.toEntity(request);
    CombinedScore savedEntity = combinedScoreRepository.save(groupedProducts);
    return combinedScoreMapper.toResponse(savedEntity);
  }

  public void cancelGrouping(Long id) {
    if (!combinedScoreRepository.existsById(id)) {
      throw new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado.");
    }
    CombinedScore savedEntity =
        combinedScoreRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado."));
    Client client =
        clientRepository
            .findById(savedEntity.getClientId())
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "Cliente com ID " + savedEntity.getClientId() + " não encontrado."));
    BigDecimal newTotal = client.getTotalPurchaseValue().subtract(savedEntity.getTotalValue());
    client.setTotalPurchaseValue(newTotal);
    clientRepository.save(client);
    combinedScoreRepository.deleteById(id);
  }

  public Page<CombinedScoreResponse> listGroupings(Long clientId, Pageable pageable) {
    Page<CombinedScore> groupings;

    if (clientId != null) {
      groupings = combinedScoreRepository.findByClientId(clientId, pageable);
    } else {
      groupings = combinedScoreRepository.findAll(pageable);
    }

    return groupings.map(combinedScoreMapper::toResponse);
  }
}
