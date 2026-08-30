package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hortifruti.sl.hortifruti.model.purchase.StatusMatchItemTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoClienteItem;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteItemRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotaPrecoOficialCheckerTest {

  @Mock private TabelaPrecoClienteRepository tabelaPrecoClienteRepository;
  @Mock private TabelaPrecoClienteItemRepository tabelaPrecoClienteItemRepository;

  private NotaPrecoOficialChecker checker;

  @BeforeEach
  void setUp() {
    checker =
        new NotaPrecoOficialChecker(tabelaPrecoClienteRepository, tabelaPrecoClienteItemRepository);
  }

  @Test
  void semTabelaConfirmadaParaAPeriodoRetornaVazioNuncaEstima() {
    when(tabelaPrecoClienteRepository
            .findFirstByClienteIdAndVigenciaInicioLessThanEqualAndVigenciaFimGreaterThanEqualAndStatusOrderByVersaoDesc(
                1L,
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 15),
                StatusTabelaPreco.CONFIRMADA))
        .thenReturn(Optional.empty());

    assertThat(checker.precoOficial(1L, LocalDate.of(2026, 9, 15), 5L)).isEmpty();
    assertThat(checker.existeTabelaConfirmadaParaData(1L, LocalDate.of(2026, 9, 15))).isFalse();
  }

  @Test
  void comTabelaConfirmadaMasProdutoSemPrecoNoMesRetornaVazio() {
    TabelaPrecoCliente tabela = TabelaPrecoCliente.builder().id(10L).build();
    when(tabelaPrecoClienteRepository
            .findFirstByClienteIdAndVigenciaInicioLessThanEqualAndVigenciaFimGreaterThanEqualAndStatusOrderByVersaoDesc(
                1L,
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 15),
                StatusTabelaPreco.CONFIRMADA))
        .thenReturn(Optional.of(tabela));
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteIdAndFiscalProductId(10L, 5L))
        .thenReturn(
            Optional.of(
                TabelaPrecoClienteItem.builder()
                    .fiscalProductId(5L)
                    .preco(null)
                    .statusMatch(StatusMatchItemTabelaPreco.CONFIRMADO)
                    .build()));

    assertThat(checker.precoOficial(1L, LocalDate.of(2026, 9, 15), 5L)).isEmpty();
  }

  @Test
  void retornaPrecoOficialQuandoTabelaEItemExistem() {
    TabelaPrecoCliente tabela = TabelaPrecoCliente.builder().id(10L).build();
    when(tabelaPrecoClienteRepository
            .findFirstByClienteIdAndVigenciaInicioLessThanEqualAndVigenciaFimGreaterThanEqualAndStatusOrderByVersaoDesc(
                1L,
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 15),
                StatusTabelaPreco.CONFIRMADA))
        .thenReturn(Optional.of(tabela));
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteIdAndFiscalProductId(10L, 5L))
        .thenReturn(
            Optional.of(
                TabelaPrecoClienteItem.builder()
                    .fiscalProductId(5L)
                    .preco(new BigDecimal("12.99"))
                    .statusMatch(StatusMatchItemTabelaPreco.CONFIRMADO)
                    .build()));

    assertThat(checker.precoOficial(1L, LocalDate.of(2026, 9, 15), 5L))
        .hasValueSatisfying(preco -> assertThat(preco).isEqualByComparingTo("12.99"));
  }
}
