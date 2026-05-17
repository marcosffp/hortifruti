package com.hortifruti.sl.hortifruti.repository.invoice;

import com.hortifruti.sl.hortifruti.model.invoice.FiscalNoteXmlStorage;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalNoteXmlStorageRepository
    extends JpaRepository<FiscalNoteXmlStorage, Long> {

  Optional<FiscalNoteXmlStorage> findByRef(String ref);

  boolean existsByRef(String ref);

  List<FiscalNoteXmlStorage> findByIssuedAtBetweenOrderByIssuedAtDesc(
      LocalDate startDate, LocalDate endDate);
}
