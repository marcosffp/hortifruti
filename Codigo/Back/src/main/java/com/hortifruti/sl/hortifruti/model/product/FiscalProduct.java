package com.hortifruti.sl.hortifruti.model.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fiscal_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false, unique = true)
  private String code;

  @NotBlank
  @Column(nullable = false)
  private String description;

  @NotBlank
  @Column(nullable = false)
  private String ncm;

  @NotBlank
  @Column(nullable = false)
  private String cfop;

  @NotBlank
  @Column(nullable = false)
  private String icms;

  @NotBlank
  @Column(name = "unidade_comercial", nullable = false)
  private String unidadeComercial;

  @NotBlank
  @Column(name = "unidade_tributavel", nullable = false)
  private String unidadeTributavel;

  @Column(name = "updated_at", nullable = false)
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
