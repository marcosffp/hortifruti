package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.DispositivoVinculado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispositivoVinculadoRepository extends JpaRepository<DispositivoVinculado, Long> {
  Optional<DispositivoVinculado> findByTokenHashAndRevogadoEmIsNull(String tokenHash);

  List<DispositivoVinculado> findByUserIdAndRevogadoEmIsNullOrderByPareadoEmDesc(Long userId);

  Optional<DispositivoVinculado> findByIdAndUserId(Long id, Long userId);
}
