package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hortifruti.sl.hortifruti.exception.purchase.TabelaPrecoClienteEstadoInvalidoException;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.purchase.StatusMatchItemTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoClienteItem;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteItemRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TabelaPrecoClienteExportServiceTest {

  @Mock private TabelaPrecoClienteRepository tabelaPrecoClienteRepository;
  @Mock private TabelaPrecoClienteItemRepository tabelaPrecoClienteItemRepository;
  @Mock private FiscalProductRepository fiscalProductRepository;

  private TabelaPrecoClienteExportService service;

  @BeforeEach
  void setUp() {
    service =
        new TabelaPrecoClienteExportService(
            tabelaPrecoClienteRepository,
            tabelaPrecoClienteItemRepository,
            fiscalProductRepository);
  }

  @Test
  void bloqueiaExportacaoDeTabelaNaoConfirmada() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(
            Optional.of(
                TabelaPrecoCliente.builder().id(1L).status(StatusTabelaPreco.EM_REVISAO).build()));

    assertThatThrownBy(() -> service.montarLinhas(1L))
        .isInstanceOf(TabelaPrecoClienteEstadoInvalidoException.class);
  }

  @Test
  void exportaLinhasDeTabelaConfirmadaExcluindoSemCorrespondencia() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(
            Optional.of(
                TabelaPrecoCliente.builder().id(1L).status(StatusTabelaPreco.CONFIRMADA).build()));
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(1L))
        .thenReturn(
            List.of(
                TabelaPrecoClienteItem.builder()
                    .fiscalProductId(5L)
                    .preco(new BigDecimal("12.99"))
                    .statusMatch(StatusMatchItemTabelaPreco.CONFIRMADO)
                    .build(),
                TabelaPrecoClienteItem.builder()
                    .fiscalProductId(null)
                    .statusMatch(StatusMatchItemTabelaPreco.SEM_CORRESPONDENCIA)
                    .build()));
    FiscalProduct produto = new FiscalProduct();
    produto.setId(5L);
    produto.setCode("480");
    produto.setDescription("COUVE KG");
    when(fiscalProductRepository.findAllById(List.of(5L))).thenReturn(List.of(produto));

    List<TabelaPrecoClienteExportService.LinhaTabelaPreco> linhas = service.montarLinhas(1L);

    assertThat(linhas).hasSize(1);
    assertThat(linhas.get(0).codigo()).isEqualTo("480");
    assertThat(linhas.get(0).preco()).isEqualByComparingTo("12.99");
  }
}
