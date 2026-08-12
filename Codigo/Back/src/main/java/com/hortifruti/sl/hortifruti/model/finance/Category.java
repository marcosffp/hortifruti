package com.hortifruti.sl.hortifruti.model.finance;

/**
 * Categoria contábil atribuída automaticamente a uma {@link Transaction} bancária, por palavra-
 * chave no histórico do lançamento (ver {@code TransactionCategoryClassifier.determineCategory}).
 */
public enum Category {
  VENDAS_CARTAO,
  VENDAS_PIX,
  SERVICOS_BANCARIOS,
  FORNECEDOR,

  /**
   * Retiradas/repasses pessoais do(s) sócio(s)/família proprietária do negócio — distinto de {@link
   * #FORNECEDOR} (pagamento a terceiro) e {@link #FUNCIONARIO} (folha) porque é dinheiro saindo da
   * empresa para a própria família dona do negócio, não uma despesa operacional. Foi a única
   * categoria adicionada fora do conjunto original (todo o resto é rótulo de domínio
   * bancário/fiscal padrão) — o rótulo de exibição acentuado ("Família") fica em {@code
   * TransactionCategoryClassifier#categoryLabel}, não no nome do enum.
   */
  FAMILIA,

  FUNCIONARIO,
  SERVICOS_TELEFONICOS,
  CEMIG,
  COPASA,
  FISCAL,
  IMPOSTOS,
}
