package com.hortifruti.sl.hortifruti.model.purchase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @NotNull
  @Column(name = "purchase_date", nullable = false)
  private LocalDateTime purchaseDate;

  @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<InvoiceProduct> invoiceProducts;

  @NotNull
  @Column(name = "total", nullable = false, precision = 15, scale = 2)
  private BigDecimal total;

  /**
   * Chave no R2 da foto original da nota, só preenchida quando a compra veio de uma captura por
   * celular E o cliente exige comprovante ({@code Client#requiresPurchaseProof}) — ver {@code
   * CapturaNotaPendenteService#confirmarComoCompra}. {@code null} nos demais casos (upload de PDF,
   * lançamento manual, ou cliente que não exige comprovante).
   */
  @Column(name = "imagem_r2_key")
  private String imagemR2Key;

  /**
   * Preenchido quando esta compra passa a compor um agrupamento ({@link CombinedScore}) — ver
   * {@code CombinedScoreService#createCombinedScore}. Permite depois listar as fotos ({@link
   * #imagemR2Key}) de todas as compras de um agrupamento específico.
   */
  @Column(name = "combined_score_id")
  private Long combinedScoreId;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now(BRAZIL_ZONE);
    this.updatedAt = LocalDateTime.now(BRAZIL_ZONE);
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now(BRAZIL_ZONE);
  }
}
