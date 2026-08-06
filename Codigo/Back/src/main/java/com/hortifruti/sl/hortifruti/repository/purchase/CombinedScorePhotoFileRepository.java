package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.FileStatus;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScorePhotoFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CombinedScorePhotoFileRepository
    extends JpaRepository<CombinedScorePhotoFile, Long> {

  Optional<CombinedScorePhotoFile> findByCombinedScoreIdAndStatus(
      Long combinedScoreId, FileStatus status);
}
