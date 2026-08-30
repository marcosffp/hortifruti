package com.hortifruti.sl.hortifruti.model.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma linha do CSV do cliente dentro de uma {@link TabelaPrecoCliente}. {@code preco} fica {@code
 * null} quando o cliente não cotou esse item nesse mês (célula em branco no arquivo) — nunca vira
 * {@code 0}, que é um preço real. {@code fiscalProductId} só é aplicado a um preço oficial depois
 * que {@code statusMatch} vira {@code CONFIRMADO} ou {@code EDITADO_MANUALMENTE} — ver {@code
 * TabelaPrecoClienteReviewService}.
 */
@Entity
@Table(name = "tabela_preco_cliente_itens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TabelaPrecoClienteItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tabela_preco_cliente_id", nullable = false)
  private Long tabelaPrecoClienteId;

  @Column(name = "codigo_produto_cliente", nullable = false)
  private String codigoProdutoCliente;

  @Column(name = "nome_produto_cliente", nullable = false)
  private String nomeProdutoCliente;

  @Column(precision = 10, scale = 4)
  private BigDecimal preco;

  @Column(name = "fiscal_product_id")
  private Long fiscalProductId;

  @Column(name = "confianca_matching")
  private Double confiancaMatching;

  @Enumerated(EnumType.STRING)
  @Column(name = "status_match", nullable = false)
  private StatusMatchItemTabelaPreco statusMatch;
}
