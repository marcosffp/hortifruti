package com.hortifruti.sl.hortifruti.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hortifruti.sl.hortifruti.dto.product.ConversaoCaixaImportResponse;
import com.hortifruti.sl.hortifruti.exception.product.InvalidConversaoCaixaFileException;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.product.ProductBoxWeightHistory;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.product.ProductBoxWeightHistoryRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ConversaoCaixaImportServiceTest {

  @Mock private FiscalProductRepository fiscalProductRepository;
  @Mock private ProductBoxWeightHistoryRepository productBoxWeightHistoryRepository;

  private ConversaoCaixaImportService service;

  @BeforeEach
  void setUp() {
    service =
        new ConversaoCaixaImportService(fiscalProductRepository, productBoxWeightHistoryRepository);
  }

  private MultipartFile csv(String conteudo) {
    return new MockMultipartFile(
        "file", "conversao.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
  }

  private FiscalProduct produto(Long id, String code, BigDecimal pesoCaixaKg) {
    FiscalProduct produto = new FiscalProduct();
    produto.setId(id);
    produto.setCode(code);
    produto.setDescription("PRODUTO " + code);
    produto.setPesoCaixaKg(pesoCaixaKg);
    return produto;
  }

  @Test
  void cadastraPesoNovoQuandoProdutoNaoTinhaConversao() {
    when(fiscalProductRepository.findByCode("82")).thenReturn(Optional.of(produto(1L, "82", null)));

    ConversaoCaixaImportResponse resposta = service.importar(csv("COD,UNIDADE,KG\n82,CAIXA,20\n"));

    assertThat(resposta.cadastrados()).hasSize(1);
    assertThat(resposta.cadastrados().get(0).codigo()).isEqualTo("82");
    assertThat(resposta.cadastrados().get(0).pesoCaixaKg()).isEqualByComparingTo("20");
    assertThat(resposta.atualizados()).isEmpty();
    assertThat(resposta.semAlteracao()).isEmpty();
    verify(fiscalProductRepository).save(any());
    verify(productBoxWeightHistoryRepository).save(any());
  }

  @Test
  void atualizaEGravaHistoricoQuandoPesoMuda() {
    when(fiscalProductRepository.findByCode("95"))
        .thenReturn(Optional.of(produto(2L, "95", new BigDecimal("12"))));

    ConversaoCaixaImportResponse resposta = service.importar(csv("COD,UNIDADE,KG\n95,CAIXA,15\n"));

    assertThat(resposta.atualizados()).hasSize(1);
    assertThat(resposta.atualizados().get(0).pesoAnterior()).isEqualByComparingTo("12");
    assertThat(resposta.atualizados().get(0).pesoNovo()).isEqualByComparingTo("15");
    verify(productBoxWeightHistoryRepository).save(any(ProductBoxWeightHistory.class));
  }

  @Test
  void naoAlteraNadaQuandoPesoJaEIgual() {
    when(fiscalProductRepository.findByCode("95"))
        .thenReturn(Optional.of(produto(2L, "95", new BigDecimal("15"))));

    ConversaoCaixaImportResponse resposta = service.importar(csv("COD,UNIDADE,KG\n95,CAIXA,15\n"));

    assertThat(resposta.semAlteracao()).containsExactly("95");
    assertThat(resposta.atualizados()).isEmpty();
    assertThat(resposta.cadastrados()).isEmpty();
    verify(fiscalProductRepository, never()).save(any());
    verify(productBoxWeightHistoryRepository, never()).save(any());
  }

  @Test
  void reportaCodigoNaoEncontradoSemQuebrarOImportInteiro() {
    when(fiscalProductRepository.findByCode("999")).thenReturn(Optional.empty());
    when(fiscalProductRepository.findByCode("82")).thenReturn(Optional.of(produto(1L, "82", null)));

    ConversaoCaixaImportResponse resposta =
        service.importar(csv("COD,UNIDADE,KG\n999,CAIXA,10\n82,CAIXA,20\n"));

    assertThat(resposta.codigosNaoEncontrados()).containsExactly("999");
    assertThat(resposta.cadastrados()).hasSize(1);
  }

  @Test
  void conflitoNoMesmoArquivoAplicaOPrimeiroValorEAlerta() {
    when(fiscalProductRepository.findByCode("146"))
        .thenReturn(Optional.of(produto(3L, "146", null)));

    ConversaoCaixaImportResponse resposta =
        service.importar(csv("COD,UNIDADE,KG\n146,CAIXA,18\n146,CAIXA,15\n"));

    assertThat(resposta.conflitosNoArquivo()).hasSize(1);
    var conflito = resposta.conflitosNoArquivo().get(0);
    assertThat(conflito.codigo()).isEqualTo("146");
    assertThat(conflito.valoresEncontrados()).hasSize(2);
    assertThat(conflito.valorAplicado()).isEqualByComparingTo("18");
    // O primeiro valor (18) ainda é aplicado normalmente como cadastro.
    assertThat(resposta.cadastrados()).hasSize(1);
    assertThat(resposta.cadastrados().get(0).pesoCaixaKg()).isEqualByComparingTo("18");
  }

  @Test
  void linhasQueNaoSaoCaixaSaoIgnoradas() {
    ConversaoCaixaImportResponse resposta =
        service.importar(csv("COD,UNIDADE,KG\n82,KG,1\n66,UNID,1\n"));

    assertThat(resposta.cadastrados()).isEmpty();
    assertThat(resposta.atualizados()).isEmpty();
    assertThat(resposta.codigosNaoEncontrados()).isEmpty();
    assertThat(resposta.conflitosNoArquivo()).isEmpty();
    verify(fiscalProductRepository, never()).findByCode(any());
  }

  @Test
  void arquivoSemColunasEsperadasLancaExcecaoDeDominio() {
    assertThatThrownBy(() -> service.importar(csv("A,B,C\n1,2,3\n")))
        .isInstanceOf(InvalidConversaoCaixaFileException.class);
  }

  @Test
  void arquivoVazioLancaExcecaoDeDominio() {
    MultipartFile vazio = new MockMultipartFile("file", "vazio.csv", "text/csv", new byte[0]);

    assertThatThrownBy(() -> service.importar(vazio))
        .isInstanceOf(InvalidConversaoCaixaFileException.class);
  }
}
