package com.hortifruti.sl.hortifruti.dto.product;

import java.math.BigDecimal;

/** Produto sem {@code pesoCaixaKg} cadastrado até então, que ganhou o valor lido do arquivo. */
public record ProdutoConversaoCadastrado(String codigo, String produto, BigDecimal pesoCaixaKg) {}
