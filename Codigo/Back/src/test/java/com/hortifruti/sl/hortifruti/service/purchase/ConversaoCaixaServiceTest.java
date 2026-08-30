package com.hortifruti.sl.hortifruti.service.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hortifruti.sl.hortifruti.dto.purchase.ItemNotaExtraido;
import com.hortifruti.sl.hortifruti.dto.purchase.ProdutoSugerido;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversaoCaixaServiceTest {

  @Mock private FiscalProductRepository fiscalProductRepository;

  private ConversaoCaixaService service;

  @BeforeEach
  void setUp() {
    service = new ConversaoCaixaService(fiscalProductRepository);
  }

  private ItemNotaExtraido item(String unidade, BigDecimal quantidade, BigDecimal total) {
    return new ItemNotaExtraido(
        "MACA", quantidade, unidade, null, total, null, null, null, null, null, null, null);
  }

  private ProdutoSugerido produtoSugerido() {
    return new ProdutoSugerido(1L, "82", "MACA FUJI", 0.9);
  }

  @Test
  void meiaCaixaConverteProporcionalmente() {
    when(fiscalProductRepository.findById(1L))
        .thenReturn(Optional.of(produtoComPeso(new BigDecimal("20"))));

    var resultado =
        service.converterSeNecessario(
            item("CX", new BigDecimal("0.5"), new BigDecimal("50")), produtoSugerido());

    assertThat(resultado).isPresent();
    assertThat(resultado.get().quantidadeKg()).isEqualByComparingTo("10.000");
    assertThat(resultado.get().precoPorKg()).isEqualByComparingTo("5.00");
    assertThat(resultado.get().estimado()).isTrue();
  }

  @Test
  void caixaEMeiaConverteProporcionalmente() {
    when(fiscalProductRepository.findById(1L))
        .thenReturn(Optional.of(produtoComPeso(new BigDecimal("20"))));

    var resultado =
        service.converterSeNecessario(
            item("CX", new BigDecimal("1.5"), new BigDecimal("90")), produtoSugerido());

    assertThat(resultado).isPresent();
    assertThat(resultado.get().quantidadeKg()).isEqualByComparingTo("30.000");
    assertThat(resultado.get().precoPorKg()).isEqualByComparingTo("3.00");
  }

  @Test
  void produtoSemPesoCadastradoNaoConverte() {
    when(fiscalProductRepository.findById(1L)).thenReturn(Optional.of(produtoComPeso(null)));

    var resultado =
        service.converterSeNecessario(
            item("CX", new BigDecimal("1"), new BigDecimal("20")), produtoSugerido());

    assertThat(resultado).isEmpty();
  }

  @Test
  void semProdutoIdentificadoNaoConverte() {
    var resultado =
        service.converterSeNecessario(item("CX", new BigDecimal("1"), new BigDecimal("20")), null);

    assertThat(resultado).isEmpty();
  }

  @Test
  void unidadeDiferenteDeCaixaNaoConverte() {
    var resultado =
        service.converterSeNecessario(
            item("KG", new BigDecimal("1"), new BigDecimal("20")), produtoSugerido());

    assertThat(resultado).isEmpty();
  }

  @Test
  void quantidadeNulaNaoConverte() {
    var resultado =
        service.converterSeNecessario(item("CX", null, new BigDecimal("20")), produtoSugerido());

    assertThat(resultado).isEmpty();
  }

  @Test
  void totalNuloNaoCalculaPrecoPorKgMasConverteQuantidade() {
    when(fiscalProductRepository.findById(1L))
        .thenReturn(Optional.of(produtoComPeso(new BigDecimal("20"))));

    var resultado =
        service.converterSeNecessario(item("CX", new BigDecimal("1"), null), produtoSugerido());

    assertThat(resultado).isPresent();
    assertThat(resultado.get().quantidadeKg()).isEqualByComparingTo("20.000");
    assertThat(resultado.get().precoPorKg()).isNull();
  }

  private FiscalProduct produtoComPeso(BigDecimal peso) {
    FiscalProduct produto = new FiscalProduct();
    produto.setId(1L);
    produto.setPesoCaixaKg(peso);
    return produto;
  }
}
