package com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.dto.purchase.ProdutoSugerido;

/**
 * Item do arquivo do cliente sem mapeamento prévio, casado por matching fuzzy contra o catálogo —
 * sempre entra em revisão manual (nunca vira {@code CONFIRMADO} só por causa da confiança).
 */
public record ItemSugerido(
    Long itemId,
    String codigoProdutoCliente,
    String nomeProdutoCliente,
    ProdutoSugerido produtoSugerido,
    String confianca) {}
