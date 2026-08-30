package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.dto.purchase.ProdutoSugerido;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.util.FuzzyTextMatchUtils;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Cruza o {@code nomeProdutoCliente} (coluna {@code NOME_PR} do CSV oficial do cliente, ex.:
 * "ALFACE AMERICANA KG", "CEBOLINHA 300GR UN") com o catálogo fiscal, pra sugerir o produto certo
 * na revisão da tabela de preços — apenas uma sugestão, nunca aplicada sozinha (ver {@code
 * TabelaPrecoClienteImportService}). Deliberadamente **não** delega pro {@link
 * com.hortifruti.sl.hortifruti.service.purchase.ProdutoMatchingService}: lá o texto lido vem de OCR
 * de nota manuscrita e tem sinais de unidade/quantidade separados do Gemini pra desempatar
 * variantes vizinhas; aqui os dois lados são texto digitado limpo (planilha vs. catálogo), sem
 * esses sinais — o núcleo de normalização/fuzzy é compartilhado via {@link FuzzyTextMatchUtils}, só
 * o cálculo de score é próprio.
 */
@Service
@RequiredArgsConstructor
public class ProdutoClienteMatchingService {

  private static final Set<String> UNIT_TOKENS =
      Set.of(
          "kg", "g", "gr", "grs", "grama", "gramas", "un", "uni", "und", "unid", "unidade", "mc",
          "maco", "cx", "caixa", "dz", "duzia", "fd", "fardo", "pct", "pacote", "bandeja", "pe");

  private static final Pattern DIGITS = Pattern.compile("[0-9]+");

  private static final double FUZZY_TOKEN_RATIO_LIMIT =
      FuzzyTextMatchUtils.FUZZY_TOKEN_RATIO_LIMIT_PADRAO;

  private final FiscalProductRepository fiscalProductRepository;

  // Default em Java (não só no application.properties) pra continuar correto quando a classe é
  // instanciada direto em teste unitário, sem contexto Spring processando o @Value.
  @Value("${nota.matching.cliente-produto.limiar:0.6}")
  private double limiar = 0.6;

  @Value("${nota.matching.cliente-produto.confianca-alta:0.85}")
  private double confiancaAltaLimiar = 0.85;

  public record Resultado(ProdutoSugerido produtoSugerido, String confianca) {}

  public Resultado buscarMelhorCandidato(String nomeProdutoCliente) {
    return buscarMelhorCandidato(nomeProdutoCliente, fiscalProductRepository.findAll());
  }

  /**
   * Overload que recebe o catálogo direto, sem tocar no banco — existe pra dar pra testar a lógica
   * de score isoladamente.
   */
  public Resultado buscarMelhorCandidato(String nomeProdutoCliente, List<FiscalProduct> produtos) {
    if (nomeProdutoCliente == null || nomeProdutoCliente.isBlank() || produtos.isEmpty()) {
      return new Resultado(null, "baixa");
    }

    String normLido = FuzzyTextMatchUtils.normalize(stripDigits(nomeProdutoCliente));
    if (normLido.isBlank()) {
      return new Resultado(null, "baixa");
    }
    Set<String> tokensLido = filtrarUnidade(FuzzyTextMatchUtils.tokenize(normLido));

    FiscalProduct melhor = null;
    double melhorScore = -1;
    for (FiscalProduct produto : produtos) {
      double score = score(tokensLido, normLido, produto);
      if (score > melhorScore) {
        melhorScore = score;
        melhor = produto;
      }
    }

    if (melhor == null || melhorScore < limiar) {
      return new Resultado(null, "baixa");
    }

    String confianca = melhorScore >= confiancaAltaLimiar ? "alta" : "media";
    ProdutoSugerido sugestao =
        new ProdutoSugerido(
            melhor.getId(),
            melhor.getCode(),
            melhor.getDescription(),
            FuzzyTextMatchUtils.round(melhorScore));
    return new Resultado(sugestao, confianca);
  }

  private double score(Set<String> tokensLido, String normLido, FiscalProduct produto) {
    String normDesc = FuzzyTextMatchUtils.normalize(produto.getDescription());
    Set<String> tokensDesc = filtrarUnidade(FuzzyTextMatchUtils.tokenize(normDesc));

    long tokensLidoCasados =
        tokensLido.stream()
            .filter(
                t ->
                    FuzzyTextMatchUtils.temCorrespondenciaFuzzy(
                        t, tokensDesc, FUZZY_TOKEN_RATIO_LIMIT))
            .count();

    double base;
    if (tokensLidoCasados == 0) {
      base = 1 - FuzzyTextMatchUtils.levenshteinRatio(normLido, normDesc);
    } else {
      double cobertura = (double) tokensLidoCasados / tokensLido.size();
      long tokensDescCasados =
          tokensDesc.stream()
              .filter(
                  t ->
                      FuzzyTextMatchUtils.temCorrespondenciaFuzzy(
                          t, tokensLido, FUZZY_TOKEN_RATIO_LIMIT))
              .count();
      double precisao = tokensDesc.isEmpty() ? 1.0 : (double) tokensDescCasados / tokensDesc.size();
      base = (cobertura + precisao) / 2;
    }

    return Math.max(0, Math.min(1, base));
  }

  private Set<String> filtrarUnidade(Set<String> tokens) {
    return tokens.stream().filter(t -> !UNIT_TOKENS.contains(t)).collect(Collectors.toSet());
  }

  private String stripDigits(String text) {
    return DIGITS.matcher(text).replaceAll(" ");
  }
}
