package com.hortifruti.sl.hortifruti.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Column(name = "purchase_date", nullable = false)
  private LocalDateTime purchaseDate;

  @OneToMany(mappedBy = "purchase")
  @JsonIgnore // Ignora a relação inversa ao serializar
  private List<InvoiceProduct> invoiceProducts;

  @Column(name = "total", nullable = false)
  private BigDecimal total;

  @Column(name = "update_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
