package com.hortifruti.sl.hortifruti.model.purchase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
@Table(name = "clients")
public class Client {

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String clientName;

  @Email
  @Column(nullable = true, unique = true)
  private String email;

  @Column(nullable = true)
  private String phoneNumber;

  @NotBlank
  @Column(nullable = false)
  private String address;

  @NotBlank
  @Column(nullable = false)
  private String document;

  @Column(nullable = false)
  private boolean variablePrice;

  @Column(nullable = false)
  private boolean onlyBillet;

  /**
   * Se {@code true}, a foto original da nota é mantida no R2 quando uma compra desse cliente vem de
   * uma captura por celular (ver {@code CapturaNotaPendenteService#confirmarComoCompra}); se {@code
   * false} (padrão), só os dados extraídos são guardados e a foto é descartada.
   */
  @Column(nullable = false)
  private boolean requiresPurchaseProof;

  @Column(nullable = true)
  private String stateRegistration;

  @Column(nullable = true)
  private Integer stateIndicator;

  @Column(nullable = true)
  private String cideCode;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(
      mappedBy = "client",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JsonIgnore
  private List<Purchase> purchases;

  @Column(nullable = true)
  private LocalDate lastPurchaseDate;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now(BRAZIL_ZONE);
    this.updatedAt = LocalDateTime.now(BRAZIL_ZONE);
    this.lastPurchaseDate = null;

    if (this.cideCode == null) {
      this.cideCode = "";
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now(BRAZIL_ZONE);
  }
}
