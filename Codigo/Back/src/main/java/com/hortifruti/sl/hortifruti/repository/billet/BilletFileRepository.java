package com.hortifruti.sl.hortifruti.repository.billet;

import com.hortifruti.sl.hortifruti.model.FileStatus;
import com.hortifruti.sl.hortifruti.model.billet.BilletFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BilletFileRepository extends JpaRepository<BilletFile, Long> {

  Optional<BilletFile> findByCombinedScoreIdAndStatus(Long combinedScoreId, FileStatus status);

  /**
   * Todos os arquivos do combinedScoreId, ativos ou já cancelados (ex: por uma baixa avulsa
   * anterior) — usado pelo hard-delete em cascata do agrupamento, que precisa remover qualquer
   * arquivo remanescente do R2, não só o ativo.
   */
  List<BilletFile> findByCombinedScoreId(Long combinedScoreId);
}
