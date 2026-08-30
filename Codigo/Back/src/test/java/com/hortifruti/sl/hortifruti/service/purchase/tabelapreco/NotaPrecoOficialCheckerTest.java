package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.purchase.StatusMatchItemTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoClienteItem;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteItemRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
  @Mock private FiscalProductRepository fiscalProductRepository;

  private NotaPrecoOficialChecker checker;

  @BeforeEach
  void setUp() {
    checker =
        new NotaPrecoOficialChecker(
            tabelaPrecoClienteRepository, tabelaPrecoClienteItemRepository, fiscalProductRepository);
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

  @Test
  void precosVigentesRetornaMapaVazioSemTabelaConfirmadaParaAData() {
    when(tabelaPrecoClienteRepository
            .findFirstByClienteIdAndVigenciaInicioLessThanEqualAndVigenciaFimGreaterThanEqualAndStatusOrderByVersaoDesc(
                1L,
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 15),
                StatusTabelaPreco.CONFIRMADA))
        .thenReturn(Optional.empty());

    assertThat(checker.precosVigentesPorCodigoProduto(1L, LocalDate.of(2026, 9, 15))).isEmpty();
  }

  @Test
  void precosVigentesIgnoraItensSemPrecoOuSemProdutoCasado() {
    TabelaPrecoCliente tabela = TabelaPrecoCliente.builder().id(10L).build();
    when(tabelaPrecoClienteRepository
            .findFirstByClienteIdAndVigenciaInicioLessThanEqualAndVigenciaFimGreaterThanEqualAndStatusOrderByVersaoDesc(
                1L,
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 15),
                StatusTabelaPreco.CONFIRMADA))
        .thenReturn(Optional.of(tabela));
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(10L))
        .thenReturn(
            List.of(
                TabelaPrecoClienteItem.builder()
                    .fiscalProductId(5L)
                    .preco(new BigDecimal("12.99"))
                    .statusMatch(StatusMatchItemTabelaPreco.CONFIRMADO)
                    .build(),
                TabelaPrecoClienteItem.builder()
                    .fiscalProductId(6L)
                    .preco(null)
                    .statusMatch(StatusMatchItemTabelaPreco.CONFIRMADO)
                    .build(),
                TabelaPrecoClienteItem.builder()
                    .fiscalProductId(null)
                    .preco(new BigDecimal("3.50"))
                    .statusMatch(StatusMatchItemTabelaPreco.SEM_CORRESPONDENCIA)
                    .build()));
    when(fiscalProductRepository.findAllById(List.of(5L)))
        .thenReturn(List.of(FiscalProduct.builder().id(5L).code("490").build()));

    Map<String, BigDecimal> precos =
        checker.precosVigentesPorCodigoProduto(1L, LocalDate.of(2026, 9, 15));

    assertThat(precos).hasSize(1);
    assertThat(precos.get("490")).isEqualByComparingTo("12.99");
  }
}
