package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoClienteItem;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteItemRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private final FiscalProductRepository fiscalProductRepository;

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

  /**
   * Preço oficial confirmado de cada produto do catálogo (por {@code code}, a mesma referência que
   * o frontend já usa pra um {@link FiscalProduct} — ver {@code ProductAutocompleteField}) na
   * tabela vigente do cliente pra essa data. Usado pra sincronizar o preço da linha na revisão de
   * nota assim que o usuário escolhe/corrige o cliente ou o produto de um item, sem esperar
   * reprocessar a extração inteira. Mesmo critério de vigência de {@link #precoOficial}; mapa vazio
   * se não existir tabela CONFIRMADA cobrindo a data.
   */
  public Map<String, BigDecimal> precosVigentesPorCodigoProduto(Long clienteId, LocalDate data) {
    if (clienteId == null || data == null) {
      return Map.of();
    }

    Optional<TabelaPrecoCliente> tabela = buscarTabelaVigente(clienteId, data);
    if (tabela.isEmpty()) {
      return Map.of();
    }

    List<TabelaPrecoClienteItem> itens =
        tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(tabela.get().getId());

    List<Long> idsProdutos =
        itens.stream()
            .map(TabelaPrecoClienteItem::getFiscalProductId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    Map<Long, FiscalProduct> produtosPorId = new HashMap<>();
    fiscalProductRepository.findAllById(idsProdutos).forEach(p -> produtosPorId.put(p.getId(), p));

    Map<String, BigDecimal> precosPorCodigo = new HashMap<>();
    for (TabelaPrecoClienteItem item : itens) {
      if (item.getPreco() == null || item.getFiscalProductId() == null) {
        continue;
      }
      FiscalProduct produto = produtosPorId.get(item.getFiscalProductId());
      if (produto != null) {
        precosPorCodigo.put(produto.getCode(), item.getPreco());
      }
    }
    return precosPorCodigo;
  }

  private Optional<TabelaPrecoCliente> buscarTabelaVigente(Long clienteId, LocalDate data) {
    return tabelaPrecoClienteRepository
        .findFirstByClienteIdAndVigenciaInicioLessThanEqualAndVigenciaFimGreaterThanEqualAndStatusOrderByVersaoDesc(
            clienteId, data, data, StatusTabelaPreco.CONFIRMADA);
  }
}
