package com.hortifruti.sl.hortifruti.model.billet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "billet_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BilletFile {

  public enum Status {
    ACTIVE,
    CANCELLED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "combined_score_id", nullable = false)
  private Long combinedScoreId;

  @Column(name = "object_key", nullable = false, unique = true, length = 500)
  private String objectKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private Status status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    if (this.status == null) {
      this.status = Status.ACTIVE;
    }
  }
}
