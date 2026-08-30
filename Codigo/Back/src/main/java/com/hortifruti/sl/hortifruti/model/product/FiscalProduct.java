package com.hortifruti.sl.hortifruti.model.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

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

  /**
   * Peso de referência (kg) de uma caixa desse produto — cadastrado pelo dono da loja (import via
   * CSV, ver {@code ConversaoCaixaImportService}) e usado pra converter itens em caixa (CX) pra kg
   * de forma determinística na extração de nota, em vez do Gemini estimar a cada chamada. {@code
   * null} quando o produto não tem conversão cadastrada ainda.
   */
  @Column(name = "peso_caixa_kg", precision = 10, scale = 3)
  private BigDecimal pesoCaixaKg;

  @Column(name = "peso_caixa_kg_atualizado_em")
  private LocalDateTime pesoCaixaKgAtualizadoEm;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now(BRAZIL_ZONE);
    this.updatedAt = LocalDateTime.now(BRAZIL_ZONE);
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now(BRAZIL_ZONE);
  }
}
