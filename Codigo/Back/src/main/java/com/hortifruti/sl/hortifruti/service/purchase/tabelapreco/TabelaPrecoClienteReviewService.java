package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.TabelaPrecoClienteItemResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.TabelaPrecoClienteResponse;
import com.hortifruti.sl.hortifruti.exception.product.FiscalProductNaoEncontradoException;
import com.hortifruti.sl.hortifruti.exception.purchase.TabelaPrecoClienteEstadoInvalidoException;
import com.hortifruti.sl.hortifruti.exception.purchase.TabelaPrecoClienteNaoEncontradaException;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.purchase.ClienteProdutoMapeamento;
import com.hortifruti.sl.hortifruti.model.purchase.StatusMatchItemTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoClienteItem;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.ClienteProdutoMapeamentoRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteItemRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revisão humana de uma {@link TabelaPrecoCliente} importada: confirmar/corrigir cada sugestão de
 * matching, marcar item sem correspondência, e só então travar a tabela em {@code CONFIRMADA}. É
 * aqui que a regra inegociável da spec ganha seu ponto de aplicação real — {@link #confirmarTabela}
 * recusa a transição enquanto existir item {@code SUGERIDO} sem decisão humana.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TabelaPrecoClienteReviewService {

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  private final TabelaPrecoClienteRepository tabelaPrecoClienteRepository;
  private final TabelaPrecoClienteItemRepository tabelaPrecoClienteItemRepository;
  private final ClienteProdutoMapeamentoRepository clienteProdutoMapeamentoRepository;
  private final FiscalProductRepository fiscalProductRepository;

  @Value("${nota.matching.cliente-produto.confianca-alta:0.85}")
  private double confiancaAltaLimiar = 0.85;

  @Transactional(readOnly = true)
  public TabelaPrecoClienteResponse buscarTabela(Long tabelaId) {
    TabelaPrecoCliente tabela = buscarTabelaOuFalhar(tabelaId);
    List<TabelaPrecoClienteItem> itens =
        tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(tabelaId);
    return montarResponse(tabela, itens);
  }

  @Transactional
  public TabelaPrecoClienteResponse confirmarItem(
      Long tabelaId, Long itemId, String fiscalProductCode, Long usuarioId) {
    TabelaPrecoCliente tabela = prepararItemParaEdicao(tabelaId);
    TabelaPrecoClienteItem item = buscarItemDaTabela(tabelaId, itemId);

    FiscalProduct produto =
        fiscalProductRepository
            .findByCode(fiscalProductCode)
            .orElseThrow(
                () ->
                    new FiscalProductNaoEncontradoException(
                        "Produto fiscal " + fiscalProductCode + " não encontrado."));

    boolean mesmaSugestao = Objects.equals(item.getFiscalProductId(), produto.getId());
    item.setFiscalProductId(produto.getId());
    item.setStatusMatch(
        mesmaSugestao
            ? StatusMatchItemTabelaPreco.CONFIRMADO
            : StatusMatchItemTabelaPreco.EDITADO_MANUALMENTE);
    tabelaPrecoClienteItemRepository.save(item);

    upsertMapeamento(
        tabela.getClienteId(), item.getCodigoProdutoCliente(), produto.getId(), usuarioId);

    return buscarTabela(tabelaId);
  }

  @Transactional
  public TabelaPrecoClienteResponse marcarSemCorrespondencia(Long tabelaId, Long itemId) {
    prepararItemParaEdicao(tabelaId);
    TabelaPrecoClienteItem item = buscarItemDaTabela(tabelaId, itemId);

    // Deliberadamente não grava/apaga ClienteProdutoMapeamento aqui: ausência de correspondência
    // esse mês não é uma decisão permanente — se o catálogo ganhar o produto depois, o item deve
    // voltar a passar por revisão, não ficar excluído silenciosamente pra sempre.
    item.setFiscalProductId(null);
    item.setConfiancaMatching(null);
    item.setStatusMatch(StatusMatchItemTabelaPreco.SEM_CORRESPONDENCIA);
    tabelaPrecoClienteItemRepository.save(item);

    return buscarTabela(tabelaId);
  }

  /**
   * Único atalho em lote permitido: confirma apenas itens {@code SUGERIDO} de confiança alta cuja
   * sugestão reproduz um {@link ClienteProdutoMapeamento} já confirmado por humano anteriormente
   * pra esse mesmo código de produto do cliente — uma sugestão de alta confiança sem mapeamento
   * prévio continua exigindo confirmação individual.
   */
  @Transactional
  public int confirmarEmLote(Long tabelaId, Long usuarioId) {
    TabelaPrecoCliente tabela = travarParaEdicao(tabelaId);
    List<TabelaPrecoClienteItem> itens =
        tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(tabelaId);

    int confirmados = 0;
    for (TabelaPrecoClienteItem item : itens) {
      if (item.getStatusMatch() != StatusMatchItemTabelaPreco.SUGERIDO
          || item.getFiscalProductId() == null
          || item.getConfiancaMatching() == null
          || item.getConfiancaMatching() < confiancaAltaLimiar) {
        continue;
      }

      ClienteProdutoMapeamento mapeamento =
          clienteProdutoMapeamentoRepository
              .findByClienteIdAndCodigoProdutoCliente(
                  tabela.getClienteId(), item.getCodigoProdutoCliente())
              .orElse(null);
      if (mapeamento == null
          || !mapeamento.getFiscalProductId().equals(item.getFiscalProductId())) {
        continue;
      }

      item.setStatusMatch(StatusMatchItemTabelaPreco.CONFIRMADO);
      tabelaPrecoClienteItemRepository.save(item);
      confirmados++;
    }

    log.info(
        "Confirmação em lote da tabela de preços {}: {} de {} itens sugeridos confirmados",
        tabelaId,
        confirmados,
        itens.size());
    return confirmados;
  }

  /**
   * Trava a tabela em {@code CONFIRMADA} — recusa enquanto existir item {@code SUGERIDO} sem
   * decisão humana (esse é o ponto de aplicação real da regra "nenhum vínculo automático vira preço
   * oficial sem confirmação", não só uma convenção de tela).
   */
  @Transactional
  public TabelaPrecoClienteResponse confirmarTabela(Long tabelaId, Long usuarioId) {
    TabelaPrecoCliente tabela = travarParaEdicao(tabelaId);

    if (tabelaPrecoClienteItemRepository.existsByTabelaPrecoClienteIdAndStatusMatch(
        tabelaId, StatusMatchItemTabelaPreco.SUGERIDO)) {
      throw new TabelaPrecoClienteEstadoInvalidoException(
          "Ainda existem itens sugeridos sem confirmação humana — confirme ou marque como sem"
              + " correspondência antes de fechar a tabela.");
    }

    tabela.setStatus(StatusTabelaPreco.CONFIRMADA);
    tabela.setConfirmadoEm(LocalDateTime.now(BRAZIL_ZONE));
    tabela.setConfirmadoPor(usuarioId);
    tabelaPrecoClienteRepository.save(tabela);

    return buscarTabela(tabelaId);
  }

  private void upsertMapeamento(
      Long clienteId, String codigoProdutoCliente, Long fiscalProductId, Long usuarioId) {
    ClienteProdutoMapeamento mapeamento =
        clienteProdutoMapeamentoRepository
            .findByClienteIdAndCodigoProdutoCliente(clienteId, codigoProdutoCliente)
            .orElseGet(
                () ->
                    ClienteProdutoMapeamento.builder()
                        .clienteId(clienteId)
                        .codigoProdutoCliente(codigoProdutoCliente)
                        .build());
    mapeamento.setFiscalProductId(fiscalProductId);
    mapeamento.setConfirmadoEm(LocalDateTime.now(BRAZIL_ZONE));
    mapeamento.setConfirmadoPor(usuarioId);
    clienteProdutoMapeamentoRepository.save(mapeamento);
  }

  /**
   * Trava usada pelas ações em lote/fechamento ({@link #confirmarEmLote} e {@link
   * #confirmarTabela}) — essas continuam bloqueadas numa tabela {@code CONFIRMADA}, já que reabrir
   * revisão em massa ou reconfirmar uma tabela já fechada não faz sentido.
   */
  private TabelaPrecoCliente travarParaEdicao(Long tabelaId) {
    TabelaPrecoCliente tabela = buscarTabelaOuFalhar(tabelaId);
    if (tabela.getStatus() == StatusTabelaPreco.CONFIRMADA) {
      throw new TabelaPrecoClienteEstadoInvalidoException(
          "Tabela de preços já confirmada — não pode ser editada. Reimporte o arquivo pra criar"
              + " uma nova versão.");
    }
    if (tabela.getStatus() == StatusTabelaPreco.RASCUNHO) {
      tabela.setStatus(StatusTabelaPreco.EM_REVISAO);
      tabelaPrecoClienteRepository.save(tabela);
    }
    return tabela;
  }

  /**
   * Trava usada pela edição de um item individual ({@link #confirmarItem} e {@link
   * #marcarSemCorrespondencia}) — diferente de {@link #travarParaEdicao}, permite a edição mesmo
   * com a tabela já {@code CONFIRMADA} (corrigir o vínculo de um item específico depois de fechada
   * não deveria exigir reimportar o mês inteiro) e não reabre a tabela pra {@code EM_REVISAO} nesse
   * caso.
   */
  private TabelaPrecoCliente prepararItemParaEdicao(Long tabelaId) {
    TabelaPrecoCliente tabela = buscarTabelaOuFalhar(tabelaId);
    if (tabela.getStatus() == StatusTabelaPreco.RASCUNHO) {
      tabela.setStatus(StatusTabelaPreco.EM_REVISAO);
      tabelaPrecoClienteRepository.save(tabela);
    }
    return tabela;
  }

  private TabelaPrecoCliente buscarTabelaOuFalhar(Long tabelaId) {
    return tabelaPrecoClienteRepository
        .findById(tabelaId)
        .orElseThrow(
            () ->
                new TabelaPrecoClienteNaoEncontradaException(
                    "Tabela de preços " + tabelaId + " não encontrada."));
  }

  private TabelaPrecoClienteItem buscarItemDaTabela(Long tabelaId, Long itemId) {
    TabelaPrecoClienteItem item =
        tabelaPrecoClienteItemRepository
            .findById(itemId)
            .orElseThrow(
                () ->
                    new TabelaPrecoClienteNaoEncontradaException(
                        "Item " + itemId + " não encontrado."));
    if (!item.getTabelaPrecoClienteId().equals(tabelaId)) {
      throw new TabelaPrecoClienteNaoEncontradaException(
          "Item " + itemId + " não pertence à tabela " + tabelaId + ".");
    }
    return item;
  }

  private TabelaPrecoClienteResponse montarResponse(
      TabelaPrecoCliente tabela, List<TabelaPrecoClienteItem> itens) {
    Map<Long, FiscalProduct> produtosPorId = new LinkedHashMap<>();
    List<Long> idsProdutos =
        itens.stream()
            .map(TabelaPrecoClienteItem::getFiscalProductId)
            .filter(Objects::nonNull)
            .toList();
    if (!idsProdutos.isEmpty()) {
      for (FiscalProduct produto : fiscalProductRepository.findAllById(idsProdutos)) {
        produtosPorId.put(produto.getId(), produto);
      }
    }

    List<TabelaPrecoClienteItemResponse> itensResponse =
        itens.stream()
            .map(
                item -> {
                  FiscalProduct produto = produtosPorId.get(item.getFiscalProductId());
                  return new TabelaPrecoClienteItemResponse(
                      item.getId(),
                      item.getCodigoProdutoCliente(),
                      item.getNomeProdutoCliente(),
                      item.getPreco(),
                      item.getFiscalProductId(),
                      produto == null ? null : produto.getCode(),
                      produto == null ? null : produto.getDescription(),
                      item.getConfiancaMatching(),
                      item.getStatusMatch());
                })
            .toList();

    return new TabelaPrecoClienteResponse(
        tabela.getId(),
        tabela.getClienteId(),
        tabela.getCompetenciaMes(),
        tabela.getCompetenciaAno(),
        tabela.getVigenciaInicio(),
        tabela.getVigenciaFim(),
        tabela.getVersao(),
        tabela.getStatus(),
        tabela.getImportadoEm(),
        tabela.getConfirmadoEm(),
        itensResponse);
  }
}
