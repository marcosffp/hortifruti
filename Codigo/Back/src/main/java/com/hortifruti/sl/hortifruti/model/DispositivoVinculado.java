package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "user_id", nullable = false)
  private Long userId;

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
    this.pareadoEm = LocalDateTime.now();
  }
}
