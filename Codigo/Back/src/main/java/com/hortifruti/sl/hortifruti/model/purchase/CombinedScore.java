package com.hortifruti.sl.hortifruti.model.purchase;

import com.hortifruti.sl.hortifruti.model.enumeration.Status;
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

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @Column(name = "has_billet", nullable = false)
  private boolean hasBillet;

  @Column(name = "has_invoice", nullable = false)
  private boolean hasInvoice;

  @Column(name = "our_number", nullable = true)
  private String ourNumber_sicoob;

  @Column(name = "your_number", nullable = true)
  private String yourNumber;

  @OneToMany(mappedBy = "combinedScore", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GroupedProduct> groupedProducts;

  @PrePersist
  protected void onCreate() {
    this.confirmedAt = LocalDate.now();
    this.dueDate = this.confirmedAt.plusDays(20);
    this.status = Status.PENDENTE;
    this.hasBillet = false;
    this.hasInvoice = false;
  }
}
