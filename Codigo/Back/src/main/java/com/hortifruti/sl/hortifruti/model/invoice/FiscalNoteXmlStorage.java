package com.hortifruti.sl.hortifruti.model.invoice;

import com.hortifruti.sl.hortifruti.model.enumeration.FileStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "fiscal_note_xml_storage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalNoteXmlStorage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ref", nullable = false, unique = true, length = 100)
  private String ref;

  @Column(name = "nf_number", nullable = false, length = 50)
  private String nfNumber;

  @Column(name = "client_name", nullable = false, length = 255)
  private String clientName;

  @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalValue;

  @Column(name = "issued_at", nullable = false)
  private LocalDate issuedAt;

  /** Legado: XMLs salvos antes da migração para R2. Novos registros usam {@link #objectKey}. */
  @Lob
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
    this.createdAt = LocalDateTime.now();
    if (this.status == null) {
      this.status = FileStatus.ACTIVE;
    }
  }
}
