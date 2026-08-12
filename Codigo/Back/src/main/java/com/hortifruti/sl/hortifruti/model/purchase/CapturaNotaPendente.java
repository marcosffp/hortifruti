package com.hortifruti.sl.hortifruti.model.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Foto de nota enviada por um dispositivo vinculado (ou diretamente pelo próprio usuário, pra teste
 * sem celular), com extração processada em segundo plano — a resposta HTTP do upload não espera o
 * Gemini. {@code extracaoJson} é o {@code NotaExtracaoResponse} serializado, preenchido só quando
 * {@code status = PRONTA}; {@code dispositivoId} é nulo quando a captura veio de um usuário
 * autenticado normalmente (JWT), não de um dispositivo vinculado.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "capturas_nota_pendentes",
    indexes = @Index(name = "idx_capturas_nota_pendentes_usuario_id", columnList = "usuario_id"))
public class CapturaNotaPendente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "dispositivo_id")
  private Long dispositivoId;

  @NotBlank
  @Column(name = "r2_key", nullable = false)
  private String r2Key;

  @NotBlank
  @Column(name = "content_type", nullable = false)
  private String contentType;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusCaptura status;

  @Column(name = "extracao_json", columnDefinition = "TEXT")
  private String extracaoJson;

  @Column(name = "mensagem_erro")
  private String mensagemErro;

  @Column(name = "criada_em", nullable = false, updatable = false)
  private LocalDateTime criadaEm;

  @Column(name = "atualizada_em")
  private LocalDateTime atualizadaEm;

  @PrePersist
  protected void onCreate() {
    this.criadaEm = LocalDateTime.now();
    this.atualizadaEm = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.atualizadaEm = LocalDateTime.now();
  }
}
