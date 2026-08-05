package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.CapturaNotaPendente;
import com.hortifruti.sl.hortifruti.model.purchase.StatusCaptura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapturaNotaPendenteRepository extends JpaRepository<CapturaNotaPendente, Long> {
  Optional<CapturaNotaPendente> findByIdAndUsuarioId(Long id, Long usuarioId);

  List<CapturaNotaPendente> findByUsuarioIdAndStatusInOrderByCriadaEmDesc(
      Long usuarioId, List<StatusCaptura> status);
}
