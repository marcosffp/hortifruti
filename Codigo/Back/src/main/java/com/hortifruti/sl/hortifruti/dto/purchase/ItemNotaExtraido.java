package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;

/**
 * Linha de item da nota. As cinco primeiras propriedades vêm cruas do Gemini (o Jackson
 * desserializa via o construtor canônico, deixando {@code produtoSugerido}/{@code confianca}/os
 * campos de conversão como {@code null} porque não estão no JSON do Gemini); as demais são
 * preenchidas depois, pelo {@code ProdutoMatchingService}/checagem de consistência (Etapas 3 e 4 da
 * spec) e pelo {@code ConversaoCaixaService} (conversão determinística caixa→kg).
 *
 * <p>Deliberadamente só um construtor (o canônico): um segundo construtor aqui — mesmo só delegando
 * pro canônico — já foi o suficiente pra confundir a desserialização de record do Jackson pra esse
 * tipo (campos depois do 3º vinham {@code null} mesmo presentes no JSON).
 */
public record ItemNotaExtraido(
    String produtoLido,
    BigDecimal quantidade,
    String unidade,
    BigDecimal precoUnitario,
    BigDecimal total,
    ProdutoSugerido produtoSugerido,
    String confianca,
    BigDecimal quantidadeKgConvertida,
    BigDecimal precoPorKgConvertido,
    Boolean conversaoEstimada,
    BigDecimal precoOficialTabela,
    Boolean divergenciaPreco) {

  public ItemNotaExtraido comProdutoEConfianca(ProdutoSugerido produtoSugerido, String confianca) {
    return new ItemNotaExtraido(
        produtoLido,
        quantidade,
        unidade,
        precoUnitario,
        total,
        produtoSugerido,
        confianca,
        quantidadeKgConvertida,
        precoPorKgConvertido,
        conversaoEstimada,
        precoOficialTabela,
        divergenciaPreco);
  }

  /**
   * Aplica o resultado da conversão determinística caixa→kg ({@code
   * ConversaoCaixaService#converterSeNecessario}) — {@code quantidadeKgConvertida}/{@code
   * precoPorKgConvertido} são baseados no peso médio cadastrado do produto, não no peso real
   * daquela caixa específica, por isso {@code conversaoEstimada} sempre vira {@code true} aqui.
   */
  public ItemNotaExtraido comConversaoCaixa(
      BigDecimal quantidadeKgConvertida, BigDecimal precoPorKgConvertido) {
    return new ItemNotaExtraido(
        produtoLido,
        quantidade,
        unidade,
        precoUnitario,
        total,
        produtoSugerido,
        confianca,
        quantidadeKgConvertida,
        precoPorKgConvertido,
        true,
        precoOficialTabela,
        divergenciaPreco);
  }

  /**
   * Preço oficial da {@code TabelaPrecoCliente CONFIRMADA} que cobre a data da nota, pro produto já
   * casado com o catálogo — usado só pra exibir/sinalizar divergência na revisão (Etapa 3/4 da spec
   * de captura + cross-check da tabela de preços); o preço de fato persistido é
   * recalculado/sobrescrito de novo, server-side, em {@code PurchaseService#createManualPurchase} —
   * não confie só neste campo como fonte da verdade.
   */
  public ItemNotaExtraido comPrecoOficialTabela(
      BigDecimal precoOficialTabela, Boolean divergenciaPreco) {
    return new ItemNotaExtraido(
        produtoLido,
        quantidade,
        unidade,
        precoUnitario,
        total,
        produtoSugerido,
        confianca,
        quantidadeKgConvertida,
        precoPorKgConvertido,
        conversaoEstimada,
        precoOficialTabela,
        divergenciaPreco);
  }
}
