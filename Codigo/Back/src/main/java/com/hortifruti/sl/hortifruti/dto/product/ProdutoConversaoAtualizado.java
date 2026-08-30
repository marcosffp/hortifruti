package com.hortifruti.sl.hortifruti.dto.product;

import java.math.BigDecimal;

/** Produto que já tinha {@code pesoCaixaKg} cadastrado e recebeu um valor diferente do arquivo. */
public record ProdutoConversaoAtualizado(
    String codigo, String produto, BigDecimal pesoAnterior, BigDecimal pesoNovo) {}
