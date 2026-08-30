package com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco;

/** Item do arquivo do cliente sem nenhum candidato aceitável no catálogo interno. */
public record ItemSemCorrespondencia(
    Long itemId, String codigoProdutoCliente, String nomeProdutoCliente) {}
