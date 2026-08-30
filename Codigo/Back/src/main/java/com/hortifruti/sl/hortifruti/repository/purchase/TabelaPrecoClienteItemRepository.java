package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.StatusMatchItemTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoClienteItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TabelaPrecoClienteItemRepository
    extends JpaRepository<TabelaPrecoClienteItem, Long> {

  List<TabelaPrecoClienteItem> findByTabelaPrecoClienteId(Long tabelaPrecoClienteId);

  boolean existsByTabelaPrecoClienteIdAndStatusMatch(
      Long tabelaPrecoClienteId, StatusMatchItemTabelaPreco statusMatch);

  Optional<TabelaPrecoClienteItem> findByTabelaPrecoClienteIdAndFiscalProductId(
      Long tabelaPrecoClienteId, Long fiscalProductId);
}
