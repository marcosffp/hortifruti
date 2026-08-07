package com.hortifruti.sl.hortifruti.model.purchase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Column(name = "purchase_date", nullable = false)
  private LocalDateTime purchaseDate;

  @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<InvoiceProduct> invoiceProducts;

  @Column(name = "total", nullable = false)
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

  @Column(name = "update_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
