package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TabelaPrecoClienteRepository extends JpaRepository<TabelaPrecoCliente, Long> {

  /** Mais recente primeiro — o índice 0, se houver, é a versão vigente daquela competência. */
  List<TabelaPrecoCliente> findByClienteIdAndCompetenciaAnoAndCompetenciaMesOrderByVersaoDesc(
      Long clienteId, int competenciaAno, int competenciaMes);

  List<TabelaPrecoCliente> findByClienteIdOrderByCompetenciaAnoDescCompetenciaMesDescVersaoDesc(
      Long clienteId);

  /**
   * A tabela CONFIRMADA (havendo mais de uma versão confirmada por algum motivo histórico, a de
   * maior {@code versao} vence) cuja vigência cobre {@code data} — usada no cross-check de preço de
   * nota ({@code NotaPrecoOficialChecker}) e na exportação.
   */
  Optional<TabelaPrecoCliente>
      findFirstByClienteIdAndVigenciaInicioLessThanEqualAndVigenciaFimGreaterThanEqualAndStatusOrderByVersaoDesc(
          Long clienteId, LocalDate data1, LocalDate data2, StatusTabelaPreco status);
}
