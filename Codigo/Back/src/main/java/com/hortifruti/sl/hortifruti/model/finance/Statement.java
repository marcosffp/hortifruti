package com.hortifruti.sl.hortifruti.model.finance;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "statements")
public class Statement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Lob
  @Column(name = "file_path", columnDefinition = "LONGBLOB")
  private byte[] filePath;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  private Bank bank;

  @Column(name = "update_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "statement")
  private List<Transaction> transactions;

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
