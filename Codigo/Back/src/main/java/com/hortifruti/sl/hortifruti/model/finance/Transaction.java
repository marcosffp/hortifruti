package com.hortifruti.sl.hortifruti.model.finance;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "statement_id", nullable = false)
  private Statement statement;

  @NotNull
  @Column(nullable = false)
  private LocalDate transactionDate;

  @Column(nullable = true)
  private String codHistory;

  @NotBlank
  @Column(nullable = false, length = 500)
  private String history;

  @NotNull
  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @NotNull
  @Enumerated(EnumType.STRING)
  // columnDefinition explícito: sem isso, o dialect MySQL do Hibernate mapeia
  // @Enumerated(STRING) para um ENUM nativo do banco (lista de valores fixada na criação da coluna)
  // em vez de VARCHAR. ddl-auto=update nunca reescreve essa lista quando um valor do enum Java é
  // renomeado depois, então uma renomeação (ex.: FAMÍLIA -> FAMILIA, ver V15 em db/migration) trava
  // pra sempre com o valor antigo em produção, mesmo após corrigir o enum e rodar uma migration só
  // de dado (V9/V12/V13/V14 tentaram e nenhuma funcionou por causa disso).
  @Column(nullable = false, columnDefinition = "VARCHAR(32)")
  private Category category;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType transactionType;

  @Column(nullable = true)
  private String document;

  @Column(nullable = true)
  private String sourceAgency;

  @Column(nullable = true)
  private String batch;

  @NotBlank
  @Column(unique = true, nullable = false)
  private String hash;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(nullable = false, updatable = false)
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
