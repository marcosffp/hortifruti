/**
 * Regra de modelagem de relacionamento entre entidades:
 *
 * <p>Use {@code @ManyToOne}/{@code @OneToMany} (associação JPA de verdade) quando a entidade
 * "pertence" a outra dentro do mesmo domínio e é comum navegar dela para a outra (ex.: {@code
 * Purchase.client}).
 *
 * <p>Use uma FK crua ({@code Long xId}) apenas quando a referência for fraca, cross-domain, ou
 * quando carregar a entidade relacionada não for necessário no caso de uso comum (ex.: {@code
 * CombinedScore.clientId}, {@code BilletFile.combinedScoreId}).
 *
 * <p>Entidades novas devem seguir uma dessas duas opções conscientemente — não misturar os dois
 * estilos para o mesmo relacionamento em classes diferentes do mesmo domínio sem justificar.
 */
package com.hortifruti.sl.hortifruti.model;
