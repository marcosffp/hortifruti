package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.exception.purchase.TabelaPrecoClienteEstadoInvalidoException;
import com.hortifruti.sl.hortifruti.exception.purchase.TabelaPrecoClienteNaoEncontradaException;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.purchase.StatusMatchItemTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.StatusTabelaPreco;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoCliente;
import com.hortifruti.sl.hortifruti.model.purchase.TabelaPrecoClienteItem;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteItemRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.TabelaPrecoClienteRepository;
import com.hortifruti.sl.hortifruti.service.invoice.tax.PdfReportSupport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exporta uma {@link TabelaPrecoCliente} {@code CONFIRMADA} no formato interno da loja — CSV {@code
 * COD,PRODUTO,KG / PREÇO} (mesmo layout de {@code Lista maior - TABELA DE PRECOS LLINEA.csv}) e o
 * PDF equivalente (ver {@link TabelaPrecoClientePdfGenerator}). Usa o {@code code}/{@code
 * description} do {@link FiscalProduct} vinculado, não o código/nome do cliente. Itens {@code
 * SEM_CORRESPONDENCIA} não entram nas linhas exportadas (não têm código interno) — ficam
 * disponíveis pra revisão via {@code GET /{tabelaId}} normalmente, então nada desaparece
 * silenciosamente, só não é confundido com uma linha de preço válida.
 */
@Service
@RequiredArgsConstructor
public class TabelaPrecoClienteExportService {

  private static final String[] CABECALHO_CSV = {"COD", "PRODUTO", "KG / PREÇO"};

  private final TabelaPrecoClienteRepository tabelaPrecoClienteRepository;
  private final TabelaPrecoClienteItemRepository tabelaPrecoClienteItemRepository;
  private final FiscalProductRepository fiscalProductRepository;

  /**
   * Linha pronta pra exportação (CSV ou PDF) — código/descrição já resolvidos do catálogo interno.
   */
  public record LinhaTabelaPreco(String codigo, String descricao, BigDecimal preco) {}

  @Transactional(readOnly = true)
  public byte[] exportarCsv(Long tabelaId) {
    List<LinhaTabelaPreco> linhas = montarLinhas(tabelaId);

    try (ByteArrayOutputStream saida = new ByteArrayOutputStream();
        CSVPrinter printer =
            new CSVPrinter(
                new java.io.OutputStreamWriter(saida, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(CABECALHO_CSV).get())) {
      for (LinhaTabelaPreco linha : linhas) {
        printer.printRecord(linha.codigo(), linha.descricao(), formatarPreco(linha.preco()));
      }
      printer.flush();
      return saida.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Não foi possível gerar o CSV de exportação.", e);
    }
  }

  /**
   * Linhas exportáveis de uma tabela: exclui itens {@code SEM_CORRESPONDENCIA} (sem código interno
   * pra exportar) — eles continuam visíveis via {@code GET /{tabelaId}} normalmente, então nada
   * desaparece silenciosamente, só não vira linha de preço.
   */
  @Transactional(readOnly = true)
  public List<LinhaTabelaPreco> montarLinhas(Long tabelaId) {
    TabelaPrecoCliente tabela = buscarTabelaOuFalhar(tabelaId);
    if (tabela.getStatus() != StatusTabelaPreco.CONFIRMADA) {
      throw new TabelaPrecoClienteEstadoInvalidoException(
          "Só é possível exportar uma tabela de preços já CONFIRMADA — revise e confirme antes de"
              + " exportar.");
    }
    List<TabelaPrecoClienteItem> itens =
        tabelaPrecoClienteItemRepository.findByTabelaPrecoClienteId(tabelaId);

    Map<Long, FiscalProduct> produtosPorId = new LinkedHashMap<>();
    List<Long> ids =
        itens.stream()
            .map(TabelaPrecoClienteItem::getFiscalProductId)
            .filter(Objects::nonNull)
            .toList();
    if (!ids.isEmpty()) {
      for (FiscalProduct produto : fiscalProductRepository.findAllById(ids)) {
        produtosPorId.put(produto.getId(), produto);
      }
    }

    return itens.stream()
        .filter(item -> item.getStatusMatch() != StatusMatchItemTabelaPreco.SEM_CORRESPONDENCIA)
        .map(item -> Map.entry(item, produtosPorId.get(item.getFiscalProductId())))
        .filter(entry -> entry.getValue() != null)
        .map(
            entry ->
                new LinhaTabelaPreco(
                    entry.getValue().getCode(),
                    entry.getValue().getDescription(),
                    entry.getKey().getPreco()))
        .toList();
  }

  /**
   * Preço em branco no arquivo original vira célula vazia na exportação também — não inventa {@code
   * 0,00} pra um item que o cliente simplesmente não cotou esse mês.
   */
  private String formatarPreco(BigDecimal preco) {
    return preco == null ? "" : PdfReportSupport.formatValueComma(preco);
  }

  private TabelaPrecoCliente buscarTabelaOuFalhar(Long tabelaId) {
    return tabelaPrecoClienteRepository
        .findById(tabelaId)
        .orElseThrow(
            () ->
                new TabelaPrecoClienteNaoEncontradaException(
                    "Tabela de preços " + tabelaId + " não encontrada."));
  }
}
