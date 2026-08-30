package com.hortifruti.sl.hortifruti.service.product;

import com.hortifruti.sl.hortifruti.dto.product.ConflitoConversaoCaixa;
import com.hortifruti.sl.hortifruti.dto.product.ConversaoCaixaImportResponse;
import com.hortifruti.sl.hortifruti.dto.product.ProdutoConversaoAtualizado;
import com.hortifruti.sl.hortifruti.dto.product.ProdutoConversaoCadastrado;
import com.hortifruti.sl.hortifruti.exception.product.InvalidConversaoCaixaFileException;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.product.ProductBoxWeightHistory;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.product.ProductBoxWeightHistoryRepository;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Cadastra/atualiza o peso de referência (kg) de uma caixa por produto, a partir de um CSV {@code
 * COD,UNIDADE,KG} exportado da planilha de conversão que o dono da loja mantém internamente. Só as
 * linhas com {@code UNIDADE=CAIXA} interessam aqui — as demais (KG, UNID) são ignoradas nessa
 * primeira versão, já que não agregam informação nova (fator sempre 1).
 *
 * <p>Idempotente: reimportar o mesmo arquivo sem mudança nenhuma não escreve nada no banco. Cada
 * cadastro/atualização real grava uma linha em {@link ProductBoxWeightHistory} (inclusive o
 * primeiro cadastro, com {@code pesoAnterior} nulo, pra dar rastro completo desde o início).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversaoCaixaImportService {

  private static final String UNIDADE_CAIXA = "CAIXA";

  private final FiscalProductRepository fiscalProductRepository;
  private final ProductBoxWeightHistoryRepository productBoxWeightHistoryRepository;

  private record LinhaCaixa(String codigo, BigDecimal pesoKg) {}

  @Transactional
  public ConversaoCaixaImportResponse importar(MultipartFile file) {
    List<LinhaCaixa> linhas = parseCsv(file);

    Map<String, List<BigDecimal>> valoresPorCodigo = new LinkedHashMap<>();
    for (LinhaCaixa linha : linhas) {
      valoresPorCodigo.computeIfAbsent(linha.codigo(), k -> new ArrayList<>()).add(linha.pesoKg());
    }

    List<ProdutoConversaoCadastrado> cadastrados = new ArrayList<>();
    List<ProdutoConversaoAtualizado> atualizados = new ArrayList<>();
    List<String> semAlteracao = new ArrayList<>();
    List<String> codigosNaoEncontrados = new ArrayList<>();
    List<ConflitoConversaoCaixa> conflitos = new ArrayList<>();

    String origem = "Import CSV " + nomeArquivo(file) + " em " + LocalDate.now();

    for (Map.Entry<String, List<BigDecimal>> entry : valoresPorCodigo.entrySet()) {
      String codigo = entry.getKey();
      List<BigDecimal> valores = entry.getValue();
      BigDecimal valorAplicado = valores.get(0);

      List<BigDecimal> distintos = valoresDistintos(valores);
      if (distintos.size() > 1) {
        // Nunca sobrescreve silenciosamente um valor por outro: aplica o primeiro encontrado no
        // arquivo e sinaliza o conflito pro usuário revisar/corrigir a planilha (ex.: código 146
        // como 18kg e 15kg no mesmo arquivo).
        conflitos.add(new ConflitoConversaoCaixa(codigo, distintos, valorAplicado));
        log.warn(
            "Conflito de peso de caixa no import: codigo={}, valores={}, valorAplicado={}",
            codigo,
            distintos,
            valorAplicado);
      }

      Optional<FiscalProduct> produtoOpt = fiscalProductRepository.findByCode(codigo);
      if (produtoOpt.isEmpty()) {
        codigosNaoEncontrados.add(codigo);
        continue;
      }

      FiscalProduct produto = produtoOpt.get();
      BigDecimal pesoAtual = produto.getPesoCaixaKg();

      if (pesoAtual == null) {
        aplicarPeso(produto, null, valorAplicado, origem);
        cadastrados.add(
            new ProdutoConversaoCadastrado(codigo, produto.getDescription(), valorAplicado));
      } else if (pesoAtual.compareTo(valorAplicado) == 0) {
        semAlteracao.add(codigo);
      } else {
        aplicarPeso(produto, pesoAtual, valorAplicado, origem);
        atualizados.add(
            new ProdutoConversaoAtualizado(
                codigo, produto.getDescription(), pesoAtual, valorAplicado));
      }
    }

    log.info(
        "Import de conversão caixa->kg concluído: cadastrados={}, atualizados={}, semAlteracao={},"
            + " naoEncontrados={}, conflitos={}",
        cadastrados.size(),
        atualizados.size(),
        semAlteracao.size(),
        codigosNaoEncontrados.size(),
        conflitos.size());

    return new ConversaoCaixaImportResponse(
        cadastrados, atualizados, semAlteracao, codigosNaoEncontrados, conflitos);
  }

  private void aplicarPeso(
      FiscalProduct produto, BigDecimal pesoAnterior, BigDecimal pesoNovo, String origem) {
    produto.setPesoCaixaKg(pesoNovo);
    produto.setPesoCaixaKgAtualizadoEm(LocalDateTime.now());
    fiscalProductRepository.save(produto);

    productBoxWeightHistoryRepository.save(
        ProductBoxWeightHistory.builder()
            .fiscalProductId(produto.getId())
            .pesoAnterior(pesoAnterior)
            .pesoNovo(pesoNovo)
            .origem(origem)
            .build());
  }

  private List<BigDecimal> valoresDistintos(List<BigDecimal> valores) {
    List<BigDecimal> distintos = new ArrayList<>();
    for (BigDecimal valor : valores) {
      boolean jaExiste = distintos.stream().anyMatch(v -> v.compareTo(valor) == 0);
      if (!jaExiste) {
        distintos.add(valor);
      }
    }
    return distintos;
  }

  private String nomeArquivo(MultipartFile file) {
    String nome = file.getOriginalFilename();
    return nome == null || nome.isBlank() ? "arquivo" : nome;
  }

  private List<LinhaCaixa> parseCsv(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidConversaoCaixaFileException("Nenhum arquivo enviado.");
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
      List<LinhaCaixa> linhas = new ArrayList<>();
      for (CSVRecord record : parser) {
        String unidade = record.get("UNIDADE");
        if (!UNIDADE_CAIXA.equalsIgnoreCase(unidade == null ? "" : unidade.trim())) {
          continue;
        }
        String codigo = record.get("COD").trim();
        BigDecimal peso = new BigDecimal(record.get("KG").trim());
        linhas.add(new LinhaCaixa(codigo, peso));
      }
      return linhas;
    } catch (IOException e) {
      throw new InvalidConversaoCaixaFileException(
          "Não foi possível ler o arquivo CSV enviado.", e);
    } catch (IllegalArgumentException e) {
      throw new InvalidConversaoCaixaFileException(
          "Arquivo CSV inválido. Confira se tem as colunas COD, UNIDADE e KG.", e);
    }
  }
}
