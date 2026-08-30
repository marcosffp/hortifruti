package com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco;

/**
 * Item do arquivo do cliente cujo código já tinha um {@code ClienteProdutoMapeamento} confirmado em
 * mês anterior — aplicado direto, sem passar por matching fuzzy de novo.
 */
public record ItemAutoAplicado(
    String codigoProdutoCliente,
    String nomeProdutoCliente,
    String fiscalProductCodigo,
    String fiscalProductDescricao) {}
