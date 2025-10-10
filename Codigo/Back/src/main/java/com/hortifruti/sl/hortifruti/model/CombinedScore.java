package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "combined_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombinedScore {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "client_id", nullable = false)
  private Long clientId;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @Column(name = "due_date")
  private LocalDateTime dueDate;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "total_value", nullable = false)
  private BigDecimal totalValue;

  @OneToMany(mappedBy = "combinedScore", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GroupedProduct> groupedProducts;

  @PrePersist
  protected void onCreate() {
    this.confirmedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.totalValue = calculateTotalValue();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
    this.totalValue = calculateTotalValue();
  }

  private BigDecimal calculateTotalValue() {
    if (groupedProducts == null || groupedProducts.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return groupedProducts.stream()
        .map(GroupedProduct::getTotalValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
