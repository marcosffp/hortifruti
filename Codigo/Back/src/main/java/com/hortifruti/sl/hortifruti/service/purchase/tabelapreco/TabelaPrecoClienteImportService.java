package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.dto.purchase.ProdutoSugerido;
import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.ItemAutoAplicado;
import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.ItemSemCorrespondencia;
import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.ItemSugerido;
import com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco.TabelaPrecoImportResponse;
import com.hortifruti.sl.hortifruti.exception.purchase.InvalidTabelaPrecoClienteFileException;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Importa o CSV oficial de tabela de preços de um cliente (ex.: LLinea — colunas {@code
 * VALIDADE_INI, VALIDADE_FIN, PRODUTO, NOME_PR, VRUNI}), casa cada item contra o catálogo interno e
 * monta uma nova {@link TabelaPrecoCliente} em {@code RASCUNHO}, pronta pra revisão humana (ver
 * {@code TabelaPrecoClienteReviewService}).
 *
 * <p>Todo import cria uma <b>nova versão</b> pra aquela competência (mês/ano), nunca sobrescreve
 * uma versão existente — inclusive quando a versão anterior já está {@code CONFIRMADA} (o cliente
 * pode reenviar a tabela corrigida no meio do mês): a versão antiga fica intacta no histórico, e as
 * consultas de preço oficial (exportação, cross-check de nota) sempre resolvem pra versão {@code
 * CONFIRMADA} de maior número.
 *
 * <p>Cada linha cujo {@code codigoProdutoCliente} já tem um {@link ClienteProdutoMapeamento}
 * confirmado em import anterior aplica esse vínculo direto ({@code CONFIRMADO}, sem matching
 * fuzzy); linhas novas passam pelo {@link ProdutoClienteMatchingService} e **nunca** saem daqui
 * como {@code CONFIRMADO} só por causa de uma confiança alta — toda sugestão fica {@code SUGERIDO}
 * até um humano confirmar (ver {@code TabelaPrecoClienteReviewService#confirmarEmLote}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TabelaPrecoClienteImportService {

  private static final DateTimeFormatter VALIDADE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final Set<String> COLUNAS_OBRIGATORIAS =
      Set.of("VALIDADE_INI", "VALIDADE_FIN", "PRODUTO", "NOME_PR", "VRUNI");

  private final TabelaPrecoClienteRepository tabelaPrecoClienteRepository;
  private final TabelaPrecoClienteItemRepository tabelaPrecoClienteItemRepository;
  private final ClienteProdutoMapeamentoRepository clienteProdutoMapeamentoRepository;
  private final ProdutoClienteMatchingService produtoClienteMatchingService;
  private final FiscalProductRepository fiscalProductRepository;

  private record LinhaCliente(
      LocalDate vigenciaInicio,
      LocalDate vigenciaFim,
      String codigoProdutoCliente,
      String nomeProdutoCliente,
      BigDecimal preco) {}

  @Transactional
  public TabelaPrecoImportResponse importar(Long clienteId, MultipartFile file, Long usuarioId) {
    List<LinhaCliente> linhas = parseCsv(file);

    LinhaCliente primeira = linhas.get(0);
    TabelaPrecoCliente tabela =
        TabelaPrecoCliente.builder()
            .clienteId(clienteId)
            .competenciaMes(primeira.vigenciaInicio().getMonthValue())
            .competenciaAno(primeira.vigenciaInicio().getYear())
            .vigenciaInicio(primeira.vigenciaInicio())
            .vigenciaFim(primeira.vigenciaFim())
            .versao(proximaVersao(clienteId, primeira.vigenciaInicio()))
            .status(StatusTabelaPreco.RASCUNHO)
            .origemArquivoNome(nomeArquivo(file))
            .importadoPor(usuarioId)
            .build();
    tabela = tabelaPrecoClienteRepository.save(tabela);

    Set<String> codigos =
        linhas.stream()
            .map(LinhaCliente::codigoProdutoCliente)
            .collect(java.util.stream.Collectors.toSet());
    Map<String, ClienteProdutoMapeamento> mapeamentosPorCodigo = new LinkedHashMap<>();
    for (ClienteProdutoMapeamento mapeamento :
        clienteProdutoMapeamentoRepository.findByClienteIdAndCodigoProdutoClienteIn(
            clienteId, codigos)) {
      mapeamentosPorCodigo.put(mapeamento.getCodigoProdutoCliente(), mapeamento);
    }

    List<ItemAutoAplicado> autoAplicados = new ArrayList<>();
    List<ItemSugerido> sugeridosAlta = new ArrayList<>();
    List<ItemSugerido> sugeridosBaixa = new ArrayList<>();
    List<ItemSemCorrespondencia> semCorrespondencia = new ArrayList<>();
    int precosEmBranco = 0;

    List<TabelaPrecoClienteItem> itensParaSalvar = new ArrayList<>();
    for (LinhaCliente linha : linhas) {
      if (linha.preco() == null) {
        precosEmBranco++;
      }

      ClienteProdutoMapeamento mapeamento = mapeamentosPorCodigo.get(linha.codigoProdutoCliente());
      TabelaPrecoClienteItem.TabelaPrecoClienteItemBuilder itemBuilder =
          TabelaPrecoClienteItem.builder()
              .tabelaPrecoClienteId(tabela.getId())
              .codigoProdutoCliente(linha.codigoProdutoCliente())
              .nomeProdutoCliente(linha.nomeProdutoCliente())
              .preco(linha.preco());

      if (mapeamento != null) {
        TabelaPrecoClienteItem item =
            itemBuilder
                .fiscalProductId(mapeamento.getFiscalProductId())
                .statusMatch(StatusMatchItemTabelaPreco.CONFIRMADO)
                .build();
        itensParaSalvar.add(item);
        continue;
      }

      ProdutoClienteMatchingService.Resultado resultado =
          produtoClienteMatchingService.buscarMelhorCandidato(linha.nomeProdutoCliente());
      ProdutoSugerido sugerido = resultado.produtoSugerido();

      if (sugerido == null) {
        TabelaPrecoClienteItem item =
            itemBuilder.statusMatch(StatusMatchItemTabelaPreco.SEM_CORRESPONDENCIA).build();
        item = tabelaPrecoClienteItemRepository.save(item);
        semCorrespondencia.add(
            new ItemSemCorrespondencia(
                item.getId(), linha.codigoProdutoCliente(), linha.nomeProdutoCliente()));
        continue;
      }

      TabelaPrecoClienteItem item =
          itemBuilder
              .fiscalProductId(sugerido.id())
              .confiancaMatching(sugerido.score())
              .statusMatch(StatusMatchItemTabelaPreco.SUGERIDO)
              .build();
      item = tabelaPrecoClienteItemRepository.save(item);
      ItemSugerido itemSugerido =
          new ItemSugerido(
              item.getId(),
              linha.codigoProdutoCliente(),
              linha.nomeProdutoCliente(),
              sugerido,
              resultado.confianca());
      if ("alta".equals(resultado.confianca())) {
        sugeridosAlta.add(itemSugerido);
      } else {
        sugeridosBaixa.add(itemSugerido);
      }
    }

    if (!itensParaSalvar.isEmpty()) {
      tabelaPrecoClienteItemRepository.saveAll(itensParaSalvar);
      Map<Long, FiscalProduct> produtosPorId = new LinkedHashMap<>();
      for (FiscalProduct produto :
          fiscalProductRepository.findAllById(
              itensParaSalvar.stream().map(TabelaPrecoClienteItem::getFiscalProductId).toList())) {
        produtosPorId.put(produto.getId(), produto);
      }
      for (TabelaPrecoClienteItem item : itensParaSalvar) {
        FiscalProduct produto = produtosPorId.get(item.getFiscalProductId());
        autoAplicados.add(
            new ItemAutoAplicado(
                item.getCodigoProdutoCliente(),
                item.getNomeProdutoCliente(),
                produto == null ? null : produto.getCode(),
                produto == null ? null : produto.getDescription()));
      }
    }

    log.info(
        "Import de tabela de preços concluído: clienteId={}, tabelaId={}, versao={}, linhas={},"
            + " autoAplicados={}, sugeridosAlta={}, sugeridosBaixa={}, semCorrespondencia={},"
            + " precosEmBranco={}",
        clienteId,
        tabela.getId(),
        tabela.getVersao(),
        linhas.size(),
        autoAplicados.size(),
        sugeridosAlta.size(),
        sugeridosBaixa.size(),
        semCorrespondencia.size(),
        precosEmBranco);

    return new TabelaPrecoImportResponse(
        tabela.getId(),
        autoAplicados,
        sugeridosAlta,
        sugeridosBaixa,
        semCorrespondencia,
        precosEmBranco);
  }

  private int proximaVersao(Long clienteId, LocalDate vigenciaInicio) {
    List<TabelaPrecoCliente> existentes =
        tabelaPrecoClienteRepository
            .findByClienteIdAndCompetenciaAnoAndCompetenciaMesOrderByVersaoDesc(
                clienteId, vigenciaInicio.getYear(), vigenciaInicio.getMonthValue());
    return existentes.isEmpty() ? 1 : existentes.get(0).getVersao() + 1;
  }

  private String nomeArquivo(MultipartFile file) {
    String nome = file.getOriginalFilename();
    return nome == null || nome.isBlank() ? "arquivo" : nome;
  }

  private List<LinhaCliente> parseCsv(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidTabelaPrecoClienteFileException("Nenhum arquivo enviado.");
    }

    CSVFormat format =
        CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .setTrim(true)
            .get();

    try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        CSVParser parser = format.parse(reader)) {
      if (!parser.getHeaderNames().containsAll(COLUNAS_OBRIGATORIAS)) {
        throw new InvalidTabelaPrecoClienteFileException(
            "Arquivo CSV inválido. Confira se tem as colunas VALIDADE_INI, VALIDADE_FIN, PRODUTO,"
                + " NOME_PR e VRUNI.");
      }

      List<LinhaCliente> linhas = new ArrayList<>();
      for (CSVRecord record : parser) {
        LocalDate vigenciaInicio = LocalDate.parse(record.get("VALIDADE_INI"), VALIDADE_FORMAT);
        LocalDate vigenciaFim = LocalDate.parse(record.get("VALIDADE_FIN"), VALIDADE_FORMAT);
        String codigo = record.get("PRODUTO").trim();
        String nome = record.get("NOME_PR").trim();
        BigDecimal preco = parsePreco(record.get("VRUNI"));
        linhas.add(new LinhaCliente(vigenciaInicio, vigenciaFim, codigo, nome, preco));
      }

      if (linhas.isEmpty()) {
        throw new InvalidTabelaPrecoClienteFileException("Arquivo CSV sem nenhuma linha de item.");
      }
      return linhas;
    } catch (IOException e) {
      throw new InvalidTabelaPrecoClienteFileException(
          "Não foi possível ler o arquivo CSV enviado.", e);
    } catch (java.time.format.DateTimeParseException | IllegalArgumentException e) {
      throw new InvalidTabelaPrecoClienteFileException(
          "Arquivo CSV inválido: " + e.getMessage(), e);
    }
  }

  /**
   * {@code VRUNI} em branco significa "não cotado esse mês" — vira {@code null}, nunca {@code 0}
   * (que é um preço real, ex.: linha {@code ALFACE UNI,0} do formato interno). Só a checagem de
   * célula em branco decide isso; o valor numérico {@code "0"} não é tratado como ausência.
   */
  private BigDecimal parsePreco(String valorBruto) {
    if (valorBruto == null || valorBruto.isBlank()) {
      return null;
    }
    return new BigDecimal(valorBruto.trim().replace(".", "").replace(",", "."));
  }
}
