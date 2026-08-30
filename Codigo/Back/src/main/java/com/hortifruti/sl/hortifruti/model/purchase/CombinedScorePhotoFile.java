package com.hortifruti.sl.hortifruti.model.purchase;

import com.hortifruti.sl.hortifruti.model.FileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * PDF com as fotos de comprovante (uma por página) das compras que compõem um {@link CombinedScore}
 * — ver {@code CombinedScorePhotoService}. Mesmo padrão de {@code
 * com.hortifruti.sl.hortifruti.model.billet.BilletFile}.
 */
@Entity
@Table(
    name = "combined_score_photo_files",
    indexes =
        @Index(
            name = "idx_combined_score_photo_files_combined_score_id",
            columnList = "combined_score_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombinedScorePhotoFile {

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "combined_score_id", nullable = false)
  private Long combinedScoreId;

  @NotBlank
  @Column(name = "object_key", nullable = false, unique = true, length = 500)
  private String objectKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private FileStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now(BRAZIL_ZONE);
    if (this.status == null) {
      this.status = FileStatus.ACTIVE;
    }
  }
}
