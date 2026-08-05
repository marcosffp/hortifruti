package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;

/**
 * Linha de item da nota. As cinco primeiras propriedades vêm cruas do Gemini (o Jackson
 * desserializa via o construtor canônico, deixando {@code produtoSugerido}/{@code confianca} como
 * {@code null} porque não estão no JSON do Gemini); essas duas últimas são preenchidas depois, pelo
 * {@code ProdutoMatchingService}/checagem de consistência (Etapas 3 e 4 da spec).
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
    String confianca) {

  public ItemNotaExtraido comProdutoEConfianca(ProdutoSugerido produtoSugerido, String confianca) {
    return new ItemNotaExtraido(
        produtoLido, quantidade, unidade, precoUnitario, total, produtoSugerido, confianca);
  }
}
