package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hortifruti.sl.hortifruti.exception.purchase.TabelaPrecoClienteEstadoInvalidoException;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.purchase.ClienteProdutoMapeamento;
import com.hortifruti.sl.hortifruti.model.purchase.StatusMatchItemTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoClienteItem;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.ClienteProdutoMapeamentoRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteItemRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TabelaPrecoClienteReviewServiceTest {

  @Mock private TabelaPrecoClienteRepository tabelaPrecoClienteRepository;
  @Mock private TabelaPrecoClienteItemRepository tabelaPrecoClienteItemRepository;
  @Mock private ClienteProdutoMapeamentoRepository clienteProdutoMapeamentoRepository;
  @Mock private FiscalProductRepository fiscalProductRepository;

  private TabelaPrecoClienteReviewService service;

  @BeforeEach
  void setUp() {
    service =
        new TabelaPrecoClienteReviewService(
            tabelaPrecoClienteRepository,
            tabelaPrecoClienteItemRepository,
            clienteProdutoMapeamentoRepository,
            fiscalProductRepository);
  }

  private TabelaPrecoCliente tabela(StatusTabelaPreco status) {
    return TabelaPrecoCliente.builder().id(1L).clienteId(7L).status(status).build();
  }

  private TabelaPrecoClienteItem item(
      Long id, Long fiscalProductId, StatusMatchItemTabelaPreco status, Double confianca) {
    return TabelaPrecoClienteItem.builder()
        .id(id)
        .tabelaPrecoClienteId(1L)
        .codigoProdutoCliente("_A1")
        .nomeProdutoCliente("ALFACE KG")
        .fiscalProductId(fiscalProductId)
        .confiancaMatching(confianca)
        .statusMatch(status)
        .build();
  }

  @Test
  void confirmarTabelaBloqueiaComItemAindaSugerido() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.EM_REVISAO)));
    when(tabelaPrecoClienteItemRepository.existsByTabelaPrecoClienteIdAndStatusMatch(
            1L, StatusMatchItemTabelaPreco.SUGERIDO))
        .thenReturn(true);

    assertThatThrownBy(() -> service.confirmarTabela(1L, 99L))
        .isInstanceOf(TabelaPrecoClienteEstadoInvalidoException.class);
  }

  @Test
  void confirmarEmLoteBloqueiaTabelaJaConfirmada() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.CONFIRMADA)));

    assertThatThrownBy(() -> service.confirmarEmLote(1L, 99L))
        .isInstanceOf(TabelaPrecoClienteEstadoInvalidoException.class);
  }

  @Test
  void confirmarTabelaBloqueiaTabelaJaConfirmada() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.CONFIRMADA)));

    assertThatThrownBy(() -> service.confirmarTabela(1L, 99L))
        .isInstanceOf(TabelaPrecoClienteEstadoInvalidoException.class);
  }

  @Test
  void confirmarItemFuncionaMesmoComTabelaJaConfirmada() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.CONFIRMADA)));
    when(tabelaPrecoClienteItemRepository.findById(2L))
        .thenReturn(Optional.of(item(2L, 5L, StatusMatchItemTabelaPreco.CONFIRMADO, 0.9)));
    when(fiscalProductRepository.findByCode("C8")).thenReturn(Optional.of(produto(8L)));
    when(clienteProdutoMapeamentoRepository.findByClienteIdAndCodigoProdutoCliente(7L, "_A1"))
        .thenReturn(Optional.empty());
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(1L))
        .thenReturn(List.of(item(2L, 8L, StatusMatchItemTabelaPreco.EDITADO_MANUALMENTE, 0.9)));

    service.confirmarItem(1L, 2L, "C8", 99L);

    var capturado = org.mockito.ArgumentCaptor.forClass(TabelaPrecoClienteItem.class);
    org.mockito.Mockito.verify(tabelaPrecoClienteItemRepository).save(capturado.capture());
    assertThat(capturado.getValue().getStatusMatch())
        .isEqualTo(StatusMatchItemTabelaPreco.EDITADO_MANUALMENTE);
    org.mockito.Mockito.verify(tabelaPrecoClienteRepository, org.mockito.Mockito.never())
        .save(any(TabelaPrecoCliente.class));
  }

  @Test
  void marcarSemCorrespondenciaFuncionaMesmoComTabelaJaConfirmada() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.CONFIRMADA)));
    when(tabelaPrecoClienteItemRepository.findById(2L))
        .thenReturn(Optional.of(item(2L, 5L, StatusMatchItemTabelaPreco.CONFIRMADO, 0.9)));
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(1L))
        .thenReturn(List.of(item(2L, null, StatusMatchItemTabelaPreco.SEM_CORRESPONDENCIA, null)));

    service.marcarSemCorrespondencia(1L, 2L);

    org.mockito.Mockito.verifyNoInteractions(clienteProdutoMapeamentoRepository);
  }

  @Test
  void confirmarItemAceitandoSugestaoOriginalVaiParaConfirmado() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.RASCUNHO)));
    when(tabelaPrecoClienteItemRepository.findById(2L))
        .thenReturn(Optional.of(item(2L, 5L, StatusMatchItemTabelaPreco.SUGERIDO, 0.9)));
    when(fiscalProductRepository.findByCode("C5")).thenReturn(Optional.of(produto(5L)));
    when(clienteProdutoMapeamentoRepository.findByClienteIdAndCodigoProdutoCliente(7L, "_A1"))
        .thenReturn(Optional.empty());
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(1L))
        .thenReturn(List.of(item(2L, 5L, StatusMatchItemTabelaPreco.CONFIRMADO, 0.9)));

    service.confirmarItem(1L, 2L, "C5", 99L);

    var capturado = org.mockito.ArgumentCaptor.forClass(TabelaPrecoClienteItem.class);
    org.mockito.Mockito.verify(tabelaPrecoClienteItemRepository).save(capturado.capture());
    assertThat(capturado.getValue().getStatusMatch())
        .isEqualTo(StatusMatchItemTabelaPreco.CONFIRMADO);
    org.mockito.Mockito.verify(clienteProdutoMapeamentoRepository)
        .save(any(ClienteProdutoMapeamento.class));
  }

  @Test
  void confirmarItemTrocandoSugestaoVaiParaEditadoManualmente() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.RASCUNHO)));
    when(tabelaPrecoClienteItemRepository.findById(2L))
        .thenReturn(Optional.of(item(2L, 5L, StatusMatchItemTabelaPreco.SUGERIDO, 0.9)));
    when(fiscalProductRepository.findByCode("C8")).thenReturn(Optional.of(produto(8L)));
    when(clienteProdutoMapeamentoRepository.findByClienteIdAndCodigoProdutoCliente(7L, "_A1"))
        .thenReturn(Optional.empty());
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(1L))
        .thenReturn(List.of(item(2L, 8L, StatusMatchItemTabelaPreco.EDITADO_MANUALMENTE, 0.9)));

    service.confirmarItem(1L, 2L, "C8", 99L);

    var capturado = org.mockito.ArgumentCaptor.forClass(TabelaPrecoClienteItem.class);
    org.mockito.Mockito.verify(tabelaPrecoClienteItemRepository).save(capturado.capture());
    assertThat(capturado.getValue().getStatusMatch())
        .isEqualTo(StatusMatchItemTabelaPreco.EDITADO_MANUALMENTE);
  }

  @Test
  void marcarSemCorrespondenciaNaoGravaMapeamento() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.RASCUNHO)));
    when(tabelaPrecoClienteItemRepository.findById(2L))
        .thenReturn(Optional.of(item(2L, 5L, StatusMatchItemTabelaPreco.SUGERIDO, 0.7)));
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(1L))
        .thenReturn(List.of(item(2L, null, StatusMatchItemTabelaPreco.SEM_CORRESPONDENCIA, null)));

    service.marcarSemCorrespondencia(1L, 2L);

    org.mockito.Mockito.verifyNoInteractions(clienteProdutoMapeamentoRepository);
  }

  @Test
  void confirmarEmLoteIgnoraAltaConfiancaSemMapeamentoPrevio() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.EM_REVISAO)));
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(1L))
        .thenReturn(List.of(item(2L, 5L, StatusMatchItemTabelaPreco.SUGERIDO, 0.95)));
    when(clienteProdutoMapeamentoRepository.findByClienteIdAndCodigoProdutoCliente(7L, "_A1"))
        .thenReturn(Optional.empty());

    int confirmados = service.confirmarEmLote(1L, 99L);

    assertThat(confirmados).isZero();
    org.mockito.Mockito.verify(tabelaPrecoClienteItemRepository, org.mockito.Mockito.never())
        .save(any(TabelaPrecoClienteItem.class));
  }

  @Test
  void confirmarEmLoteAceitaAltaConfiancaQueReproduzMapeamentoExistente() {
    when(tabelaPrecoClienteRepository.findById(1L))
        .thenReturn(Optional.of(tabela(StatusTabelaPreco.EM_REVISAO)));
    when(tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(1L))
        .thenReturn(List.of(item(2L, 5L, StatusMatchItemTabelaPreco.SUGERIDO, 0.95)));
    when(clienteProdutoMapeamentoRepository.findByClienteIdAndCodigoProdutoCliente(7L, "_A1"))
        .thenReturn(
            Optional.of(
                ClienteProdutoMapeamento.builder()
                    .clienteId(7L)
                    .codigoProdutoCliente("_A1")
                    .fiscalProductId(5L)
                    .build()));

    int confirmados = service.confirmarEmLote(1L, 99L);

    assertThat(confirmados).isEqualTo(1);
    var capturado = org.mockito.ArgumentCaptor.forClass(TabelaPrecoClienteItem.class);
    org.mockito.Mockito.verify(tabelaPrecoClienteItemRepository).save(capturado.capture());
    assertThat(capturado.getValue().getStatusMatch())
        .isEqualTo(StatusMatchItemTabelaPreco.CONFIRMADO);
  }

  private FiscalProduct produto(Long id) {
    FiscalProduct produto = new FiscalProduct();
    produto.setId(id);
    produto.setCode("C" + id);
    produto.setDescription("PRODUTO " + id);
    return produto;
  }
}
