package com.hortifruti.sl.hortifruti.model.finance;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
@Table(name = "statements")
public class Statement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Legado: extratos salvos antes da migração para R2. Novos registros usam {@link #objectKey}. */
  @Lob
  @Column(name = "file_path", columnDefinition = "LONGBLOB")
  private byte[] filePath;

  @Column(name = "object_key", length = 500)
  private String objectKey;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  private Bank bank;

  /**
   * Origem do extrato: {@code PDF_UPLOAD} (upload manual, fluxo removido — só existe em dados
   * históricos) ou {@code API} (consulta à API do Sicoob/BB, forma atual).
   */
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private StatementOrigin origin;

  /**
   * Período (dia inicial/final) confirmado como buscado na API do banco — só preenchido pra {@code
   * origin = API}. Usado pra detectar se um período pedido de novo já foi processado, sem precisar
   * bater na API do banco de novo.
   */
  @Column(name = "period_start")
  private LocalDate periodStart;

  @Column(name = "period_end")
  private LocalDate periodEnd;

  @Column(name = "update_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "statement", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Transaction> transactions;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    if (this.origin == null) {
      this.origin = StatementOrigin.PDF_UPLOAD;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
