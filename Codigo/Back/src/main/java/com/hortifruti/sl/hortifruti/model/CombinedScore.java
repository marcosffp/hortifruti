package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
  private LocalDate confirmedAt;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "total_value", nullable = false)
  private BigDecimal totalValue;

  @Column(name = "paid", nullable = false)
  private boolean paid;

  @OneToMany(mappedBy = "combinedScore", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GroupedProduct> groupedProducts;

  @PrePersist
  protected void onCreate() {
    this.confirmedAt = LocalDate.now();
    this.dueDate = this.confirmedAt.plusDays(20);
    this.totalValue = calculateTotalValue();
    this.paid = false;
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
