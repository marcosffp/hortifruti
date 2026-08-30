package com.hortifruti.sl.hortifruti.model.purchase;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.*;

@Entity
@Table(name = "grouped_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupedProduct {

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

  @NotNull
  @Column(nullable = false, precision = 10, scale = 4)
  private BigDecimal quantity;

  @NotNull
  @Column(name = "total_value", nullable = false, precision = 12, scale = 4)
  private BigDecimal totalValue;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "combined_score_id", nullable = false)
  private CombinedScore combinedScore;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
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
