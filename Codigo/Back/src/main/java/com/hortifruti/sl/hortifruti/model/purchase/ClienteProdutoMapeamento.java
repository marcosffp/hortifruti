package com.hortifruti.sl.hortifruti.model.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Memória persistente de "De/Para": uma vez que um humano confirma que o {@code
 * codigoProdutoCliente} X de um cliente corresponde ao {@link
 * com.hortifruti.sl.hortifruti.model.product.FiscalProduct} Y, essa linha faz o próximo import
 * daquele cliente aplicar o mesmo vínculo automaticamente (sem passar por matching fuzzy de novo),
 * mesmo que o nome do produto no arquivo do cliente mude ligeiramente entre meses — a chave é o
 * código, não o nome, porque o código costuma ser estável mês a mês no sistema do cliente. Ver
 * {@code TabelaPrecoClienteImportService}/{@code TabelaPrecoClienteReviewService}.
 */
@Entity
@Table(name = "cliente_produto_mapeamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteProdutoMapeamento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cliente_id", nullable = false)
  private Long clienteId;

  @Column(name = "codigo_produto_cliente", nullable = false)
  private String codigoProdutoCliente;

  @Column(name = "fiscal_product_id", nullable = false)
  private Long fiscalProductId;

  @Column(name = "confirmado_em", nullable = false)
  private LocalDateTime confirmadoEm;

  @Column(name = "confirmado_por")
  private Long confirmadoPor;
}
