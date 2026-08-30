package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.hortifruti.sl.hortifruti.dto.purchase.ProdutoSugerido;
import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.TabelaPrecoImportResponse;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.purchase.ClienteProdutoMapeamento;
import com.hortifruti.sl.hortifruti.model.purchase.StatusMatchItemTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoClienteItem;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.ClienteProdutoMapeamentoRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteItemRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class TabelaPrecoClienteImportServiceTest {

  private static final String CABECALHO =
      "REGIAO,NOME_REGIAO,VALIDADE_INI,VALIDADE_FIN,COMPRADOR,NOME_COMPRADOR,FORNECEDOR,"
          + "NM_FORNECEDOR,PRODUTO,NOME_PR,MARCA,NOME_MARCA,QTDE,VRUNI,OBS_FORNECEDOR\n";
  private static final String VIGENCIA = "01/09/2026 00:00:00,30/09/2026 00:00:00";

  @Mock private TabelaPrecoClienteRepository tabelaPrecoClienteRepository;
  @Mock private TabelaPrecoClienteItemRepository tabelaPrecoClienteItemRepository;
  @Mock private ClienteProdutoMapeamentoRepository clienteProdutoMapeamentoRepository;
  @Mock private ProdutoClienteMatchingService produtoClienteMatchingService;
  @Mock private FiscalProductRepository fiscalProductRepository;

  private TabelaPrecoClienteImportService service;
  private final AtomicLong idSequencia = new AtomicLong(1);

  @BeforeEach
  void setUp() {
    service =
        new TabelaPrecoClienteImportService(
            tabelaPrecoClienteRepository,
            tabelaPrecoClienteItemRepository,
            clienteProdutoMapeamentoRepository,
            produtoClienteMatchingService,
            fiscalProductRepository);

    when(tabelaPrecoClienteRepository.save(any(TabelaPrecoCliente.class)))
        .thenAnswer(
            invocation -> {
              TabelaPrecoCliente tabela = invocation.getArgument(0);
              if (tabela.getId() == null) {
                tabela.setId(idSequencia.getAndIncrement());
              }
              return tabela;
            });
    org.mockito.Mockito.lenient()
        .when(tabelaPrecoClienteItemRepository.save(any(TabelaPrecoClienteItem.class)))
        .thenAnswer(
            invocation -> {
              TabelaPrecoClienteItem item = invocation.getArgument(0);
              if (item.getId() == null) {
                item.setId(idSequencia.getAndIncrement());
              }
              return item;
            });
    org.mockito.Mockito.lenient()
        .when(tabelaPrecoClienteItemRepository.saveAll(anyList()))
        .thenAnswer(
            invocation -> {
              List<TabelaPrecoClienteItem> itens = invocation.getArgument(0);
              itens.forEach(
                  item -> {
                    if (item.getId() == null) {
                      item.setId(idSequencia.getAndIncrement());
                    }
                  });
              return itens;
            });
    org.mockito.Mockito.lenient()
        .when(
            clienteProdutoMapeamentoRepository.findByClienteIdAndCodigoProdutoClienteIn(
                any(), any()))
        .thenReturn(List.of());
    org.mockito.Mockito.lenient()
        .when(
            tabelaPrecoClienteRepository
                .findByClienteIdAndCompetenciaAnoAndCompetenciaMesOrderByVersaoDesc(
                    any(),
                    org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of());
  }

  private MultipartFile csv(String linhas) {
    return new MockMultipartFile(
        "file", "llinea.csv", "text/csv", (CABECALHO + linhas).getBytes(StandardCharsets.UTF_8));
  }

  private String linha(String produto, String nome, String vrUni) {
    return "003,GRANDE BH,"
        + VIGENCIA
        + ",000000999997,HORTIFRUTI,_27540906000155,"
        + "HORTIFRUTI SANTA LUZIA LTDA,"
        + produto
        + ","
        + nome
        + ",00001,PADRAO,,\""
        + vrUni
        + "\",\n";
  }

  @Test
  void precoEmBrancoViraNuloNuncaZero() {
    when(produtoClienteMatchingService.buscarMelhorCandidato(any()))
        .thenReturn(new ProdutoClienteMatchingService.Resultado(null, "baixa"));

    TabelaPrecoImportResponse resposta =
        service.importar(1L, csv(linha("_A1", "ACELGA KG", "")), 99L);

    assertThat(resposta.precosEmBrancoNoArquivo()).isEqualTo(1);
    assertThat(resposta.semCorrespondencia()).hasSize(1);
  }

  @Test
  void precoLiteralZeroNaoViraNulo() {
    when(produtoClienteMatchingService.buscarMelhorCandidato(any()))
        .thenReturn(new ProdutoClienteMatchingService.Resultado(null, "baixa"));

    var capturado = org.mockito.ArgumentCaptor.forClass(TabelaPrecoClienteItem.class);

    service.importar(1L, csv(linha("_A2", "ALFACE UNI", "0")), 99L);

    org.mockito.Mockito.verify(tabelaPrecoClienteItemRepository).save(capturado.capture());
    assertThat(capturado.getValue().getPreco()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void itemComMapeamentoPrevioAplicaDiretoSemMatchingFuzzy() {
    when(clienteProdutoMapeamentoRepository.findByClienteIdAndCodigoProdutoClienteIn(any(), any()))
        .thenReturn(
            List.of(
                ClienteProdutoMapeamento.builder()
                    .clienteId(1L)
                    .codigoProdutoCliente("_A1")
                    .fiscalProductId(10L)
                    .build()));
    when(fiscalProductRepository.findAllById(any()))
        .thenReturn(List.of(produto(10L, "480", "COUVE KG")));

    TabelaPrecoImportResponse resposta =
        service.importar(1L, csv(linha("_A1", "COUVE KG", "12,99")), 99L);

    assertThat(resposta.autoAplicadosPorMapeamento()).hasSize(1);
    assertThat(resposta.autoAplicadosPorMapeamento().get(0).fiscalProductCodigo()).isEqualTo("480");
    org.mockito.Mockito.verifyNoInteractions(produtoClienteMatchingService);
  }

  @Test
  void sugestaoAltaConfiancaVaiParaRevisaoNuncaConfirmadoDireto() {
    when(produtoClienteMatchingService.buscarMelhorCandidato(any()))
        .thenReturn(
            new ProdutoClienteMatchingService.Resultado(
                new ProdutoSugerido(5L, "600", "ALFACE AMERICANA", 0.95), "alta"));

    TabelaPrecoImportResponse resposta =
        service.importar(1L, csv(linha("_A3", "ALFACE AMERICANA KG", "9,50")), 99L);

    assertThat(resposta.sugeridosAltaConfianca()).hasSize(1);
    assertThat(resposta.sugeridosAltaConfianca().get(0).produtoSugerido().id()).isEqualTo(5L);
    var capturado = org.mockito.ArgumentCaptor.forClass(TabelaPrecoClienteItem.class);
    org.mockito.Mockito.verify(tabelaPrecoClienteItemRepository).save(capturado.capture());
    assertThat(capturado.getValue().getStatusMatch())
        .isEqualTo(StatusMatchItemTabelaPreco.SUGERIDO);
  }

  @Test
  void reimportDaMesmaCompetenciaCriaNovaVersaoSemSobrescrever() {
    when(produtoClienteMatchingService.buscarMelhorCandidato(any()))
        .thenReturn(new ProdutoClienteMatchingService.Resultado(null, "baixa"));
    when(tabelaPrecoClienteRepository
            .findByClienteIdAndCompetenciaAnoAndCompetenciaMesOrderByVersaoDesc(1L, 2026, 9))
        .thenReturn(
            List.of(
                TabelaPrecoCliente.builder()
                    .id(1L)
                    .versao(1)
                    .status(
                        com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco.CONFIRMADA)
                    .build()));

    var capturado = org.mockito.ArgumentCaptor.forClass(TabelaPrecoCliente.class);
    service.importar(1L, csv(linha("_A4", "ALECRIM KG", "5,80")), 99L);

    org.mockito.Mockito.verify(tabelaPrecoClienteRepository).save(capturado.capture());
    assertThat(capturado.getValue().getVersao()).isEqualTo(2);
  }

  private FiscalProduct produto(Long id, String code, String description) {
    FiscalProduct produto = new FiscalProduct();
    produto.setId(id);
    produto.setCode(code);
    produto.setDescription(description);
    return produto;
  }
}
