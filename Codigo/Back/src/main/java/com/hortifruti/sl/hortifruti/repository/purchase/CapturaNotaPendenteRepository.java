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

  /**
   * Várias linhas podem compartilhar o mesmo {@code r2Key} quando uma única foto tinha mais de uma
   * nota (ver {@code CapturaExtracaoAsyncService}) — usado por {@code
   * CapturaNotaPendenteService#confirmarComoCompra} pra não apagar do R2 uma foto ainda usada por
   * uma nota-irmã não finalizada.
   */
  List<CapturaNotaPendente> findByR2Key(String r2Key);
}
