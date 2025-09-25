package com.hortifruti.sl.hortifruti.model;

import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.util.TransactionUtil;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "statement_id", nullable = false)
  private Statement statement;

  @Column(nullable = false)
  private LocalDate transactionDate;

  @Column(nullable = true)
  private String codHistory; // Corrigido o nome do campo

  @Column(nullable = false, length = 500)
  private String history;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Category category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType transactionType;

  @Column(nullable = true)
  private String document;

  @Column(nullable = true)
  private String sourceAgency;

  @Column(nullable = true)
  private String batch;

  @Column(unique = true, nullable = false)
  private String hash;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.hash = TransactionUtil.generateTransactionHash(transactionDate, document, amount, history);
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactionDate, document, amount, history);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Transaction other = (Transaction) obj;
    return Objects.equals(transactionDate, other.transactionDate)
        && Objects.equals(document, other.document)
        && Objects.equals(history, other.history)
        && Objects.equals(amount, other.amount);
  }
}
