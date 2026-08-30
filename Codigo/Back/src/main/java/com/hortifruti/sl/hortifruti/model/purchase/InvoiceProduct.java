package com.hortifruti.sl.hortifruti.model.purchase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String code;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotNull
  @Column(nullable = false, precision = 10, scale = 4)
  private BigDecimal price;

  @NotBlank
  @Column(nullable = false)
  private String unitType;

  @NotNull
  @Column(nullable = false, precision = 10, scale = 4)
  private BigDecimal quantity;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_id", nullable = false)
  @JsonIgnore
  private Purchase purchase;

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
