package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vínculo entre um celular e um usuário, criado uma única vez via pareamento por código (ver
 * DispositivoVinculadoService) e reaproveitado em toda captura de nota subsequente — o celular não
 * precisa parear de novo a cada foto. O token em claro nunca é persistido, só o hash SHA-256
 * ({@code tokenHash}), no mesmo padrão de {@link RefreshToken}. {@code revogadoEm} não nulo
 * significa dispositivo desvinculado (ex.: celular perdido/roubado).
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "dispositivos_vinculados",
    indexes = @Index(name = "idx_dispositivos_vinculados_user_id", columnList = "user_id"))
public class DispositivoVinculado {

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @NotNull
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @NotBlank
  @Column(name = "nome_dispositivo", nullable = false)
  private String nomeDispositivo;

  @Column(name = "pareado_em", nullable = false, updatable = false)
  private LocalDateTime pareadoEm;

  @Column(name = "ultimo_uso_em")
  private LocalDateTime ultimoUsoEm;

  @Column(name = "revogado_em")
  private LocalDateTime revogadoEm;

  @PrePersist
  protected void onCreate() {
    this.pareadoEm = LocalDateTime.now(BRAZIL_ZONE);
  }
}
