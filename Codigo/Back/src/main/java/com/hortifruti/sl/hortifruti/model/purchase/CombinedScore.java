package com.hortifruti.sl.hortifruti.model.purchase;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "client_id", nullable = false)
  private Long clientId;

  @Column(name = "confirmed_at")
  private LocalDate confirmedAt;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @NotNull
  @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @Column(name = "has_billet", nullable = false)
  private boolean hasBillet;

  @Column(name = "has_invoice", nullable = false)
  private boolean hasInvoice;

  @Column(name = "our_number", nullable = true)
  private String ourNumberSicoob;

  @Column(name = "your_number", nullable = true)
  private String yourNumber;

  @Column(name = "invoice_ref", nullable = true)
  private String invoiceRef;

  @OneToMany(mappedBy = "combinedScore", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GroupedProduct> groupedProducts;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (this.status == null) {
      this.status = Status.PENDENTE;
    }
    this.createdAt = LocalDateTime.now(BRAZIL_ZONE);
    this.updatedAt = LocalDateTime.now(BRAZIL_ZONE);
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now(BRAZIL_ZONE);
  }
}
