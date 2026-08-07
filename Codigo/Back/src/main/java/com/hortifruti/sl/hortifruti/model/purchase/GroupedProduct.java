package com.hortifruti.sl.hortifruti.model.purchase;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "grouped_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupedProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, precision = 10, scale = 4)
  private BigDecimal price;

  @Column(nullable = false, precision = 10, scale = 4)
  private BigDecimal quantity;

  @Column(name = "total_value", nullable = false, precision = 12, scale = 4)
  private BigDecimal totalValue;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "combined_score_id", nullable = false)
  private CombinedScore combinedScore;
}
