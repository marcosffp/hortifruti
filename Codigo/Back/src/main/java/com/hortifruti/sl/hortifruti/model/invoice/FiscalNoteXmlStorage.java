package com.hortifruti.sl.hortifruti.model.invoice;

import com.hortifruti.sl.hortifruti.model.FileStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.*;

@Entity
@Table(name = "fiscal_note_xml_storage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalNoteXmlStorage {

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(name = "ref", nullable = false, unique = true, length = 100)
  private String ref;

  @NotBlank
  @Column(name = "nf_number", nullable = false, length = 50)
  private String nfNumber;

  @NotBlank
  @Column(name = "client_name", nullable = false, length = 255)
  private String clientName;

  @NotNull
  @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalValue;

  @NotNull
  @Column(name = "issued_at", nullable = false)
  private LocalDate issuedAt;

  /** Legado: XMLs salvos antes da migração para R2. Novos registros usam {@link #objectKey}. */
  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "xml_content", columnDefinition = "LONGTEXT")
  private String xmlContent;

  @Column(name = "object_key", length = 500)
  private String objectKey;

  @Column(name = "danfe_object_key", length = 500)
  private String danfeObjectKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private FileStatus status = FileStatus.ACTIVE;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

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
