package com.hortifruti.sl.hortifruti.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoice_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(nullable = false)
  private String unitType;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "update_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne
  @JoinColumn(name = "purchase_id", nullable = false)
  @JsonIgnore // Ignora a relação inversa ao serializar
  private Purchase purchase;

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
