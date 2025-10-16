package com.hortifruti.sl.hortifruti.model;

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

  @Column(nullable = false)
  private BigDecimal price;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "total_value", nullable = false)
  private BigDecimal totalValue;

  @ManyToOne
  @JoinColumn(name = "combined_score_id", nullable = false)
  private CombinedScore combinedScore;
}
