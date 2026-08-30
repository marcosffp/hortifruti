package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteItemRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Cross-check de preço: busca o preço oficial de um {@link
 * com.hortifruti.sl.hortifruti.model.product.FiscalProduct} pra um cliente numa data, na {@link
 * TabelaPrecoCliente} {@code CONFIRMADA} (versão mais recente) cuja vigência cobre essa data. Nunca
 * estima/inventa preço — retorna vazio quando não existe tabela confirmada pro período, ou quando o
 * produto não foi cotado ({@code preco} nulo naquele item). Usado tanto pra sinalizar divergência
 * na revisão de nota ({@code GeminiExtractionService}) quanto pra sobrescrever o preço de fato
 * persistido ({@code PurchaseService#createManualPurchase} — enforcement real, não só convenção de
 * tela).
 */
@Service
@RequiredArgsConstructor
public class NotaPrecoOficialChecker {

  private final TabelaPrecoClienteRepository tabelaPrecoClienteRepository;
  private final TabelaPrecoClienteItemRepository tabelaPrecoClienteItemRepository;

  /** Existe alguma tabela CONFIRMADA daquele cliente cobrindo a data — independente do produto. */
  public boolean existeTabelaConfirmadaParaData(Long clienteId, LocalDate data) {
    return buscarTabelaVigente(clienteId, data).isPresent();
  }

  public Optional<BigDecimal> precoOficial(Long clienteId, LocalDate data, Long fiscalProductId) {
    if (clienteId == null || data == null || fiscalProductId == null) {
      return Optional.empty();
    }

    return buscarTabelaVigente(clienteId, data)
        .flatMap(
            tabela ->
                tabelaPrecoClienteItemRepository
                    .findByTabelaPrecoClienteIdAndFiscalProductId(tabela.getId(), fiscalProductId)
                    .map(item -> item.getPreco()))
        .filter(preco -> preco != null);
  }

  private Optional<TabelaPrecoCliente> buscarTabelaVigente(Long clienteId, LocalDate data) {
    return tabelaPrecoClienteRepository
        .findFirstByClienteIdAndVigenciaInicioLessThanEqualAndVigenciaFimGreaterThanEqualAndStatusOrderByVersaoDesc(
            clienteId, data, data, StatusTabelaPreco.CONFIRMADA);
  }
}
