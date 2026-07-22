package com.hortifruti.sl.hortifruti.repository.billet;

import com.hortifruti.sl.hortifruti.model.billet.BilletFile;
import com.hortifruti.sl.hortifruti.model.enumeration.FileStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BilletFileRepository extends JpaRepository<BilletFile, Long> {

  Optional<BilletFile> findByCombinedScoreIdAndStatus(
      Long combinedScoreId, FileStatus status);
}
