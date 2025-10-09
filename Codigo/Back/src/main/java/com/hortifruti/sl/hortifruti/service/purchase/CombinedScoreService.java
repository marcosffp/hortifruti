package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
import com.hortifruti.sl.hortifruti.mapper.CombinedScoreMapper;
import com.hortifruti.sl.hortifruti.model.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.CombinedScoreRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CombinedScoreService {

  private final CombinedScoreRepository combinedScoreRepository;
  private final CombinedScoreMapper combinedScoreMapper;

  public CombinedScore confirmGrouping(CombinedScoreRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new CombinedScoreException("O nome do agrupamento é obrigatório.");
    }

    CombinedScore groupedProducts = combinedScoreMapper.toEntity(request);
    return combinedScoreRepository.save(groupedProducts);
  }

  public void cancelGrouping(Long id) {
    if (!combinedScoreRepository.existsById(id)) {
      throw new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado.");
    }
    combinedScoreRepository.deleteById(id);
  }

  public Page<CombinedScore> listGroupings(String search, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    if (search != null && !search.isEmpty()) {
      return combinedScoreRepository.findByNameContainingIgnoreCase(search, pageable);
    }

    return combinedScoreRepository.findAll(pageable);
  }
}
