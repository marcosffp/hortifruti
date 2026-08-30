package com.hortifruti.sl.hortifruti.model.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro histórico de cada mudança de {@link FiscalProduct#getPesoCaixaKg()} — inclusive o
 * primeiro cadastro ({@code pesoAnterior} nulo nesse caso — gravado desde o início pra dar
 * rastreabilidade completa de quando/como cada produto ganhou conversão). Gravado pelo {@code
 * ConversaoCaixaImportService} a cada import que cadastra ou atualiza um peso.
 */
@Entity
@Table(name = "product_box_weight_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBoxWeightHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "fiscal_product_id", nullable = false)
  private Long fiscalProductId;

  @Column(name = "peso_anterior", precision = 10, scale = 3)
  private BigDecimal pesoAnterior;

  @Column(name = "peso_novo", nullable = false, precision = 10, scale = 3)
  private BigDecimal pesoNovo;

  @Column(nullable = false)
  private String origem;

  @Column(name = "criado_em", nullable = false)
  private LocalDateTime criadoEm;

  @PrePersist
  protected void onCreate() {
    this.criadoEm = LocalDateTime.now();
  }
}
