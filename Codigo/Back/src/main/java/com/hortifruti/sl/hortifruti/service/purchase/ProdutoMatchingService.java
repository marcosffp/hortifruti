package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.ProdutoSugerido;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.util.FuzzyTextMatchUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Cruza o {@code produtoLido} (texto solto vindo do Gemini) com o catálogo fiscal, pra sugerir o
 * produto certo em vez do usuário digitar de novo (Etapa 3 da spec de captura de nota).
 *
 * <p>O catálogo tem várias variantes "vizinhas" do mesmo produto — ex.: ALFACE KG, ALFACE UNI,
 * ALFACE ROXA, ALFACE ROXA KG, ALFACE AMERICANA, ALFACE AMERIC KG — que colidem num matching só por
 * similaridade de texto (todas começam com "alface"). O score aqui combina três sinais pra
 * desempatar:
 *
 * <ul>
 *   <li><b>Cobertura/precisão por token</b>: separa palavras de unidade ("kg", "uni"...) do nome em
 *       si, e penaliza tanto qualificador que o candidato tem a mais e o texto lido não mencionou
 *       (ex.: "alface" sozinho contra "ALFACE ROXA") quanto qualificador que o texto lido mencionou
 *       e o candidato não tem (ex.: "alface roxa" contra "ALFACE KG" genérica).
 *   <li><b>Fuzzy por token</b> (prefixo/Levenshtein): tolera erro de leitura tipo "couv-flor" →
 *       "Couve Flor".
 *   <li><b>Pista de unidade</b>: quando o Gemini extrai a unidade (ou a quantidade tem parte
 *       fracionária, sinal característico de peso pesado em balança) e ela bate/não bate com {@code
 *       unidade_comercial} do candidato, isso desempata ALFACE KG vs. ALFACE UNI mesmo quando o
 *       nome sozinho é idêntico nos dois.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProdutoMatchingService {

  private static final Set<String> UNIT_TOKENS =
      Set.of(
          "kg", "uni", "un", "und", "unid", "unidade", "cx", "caixa", "dz", "duzia", "fd", "fardo",
          "pct", "pacote", "bandeja", "maco", "pe");

  private static final Pattern PARENTHETICAL = Pattern.compile("\\([^)]*\\)");
  private static final Pattern DIGITS = Pattern.compile("[0-9]+");

  private static final double FUZZY_TOKEN_RATIO_LIMIT =
      FuzzyTextMatchUtils.FUZZY_TOKEN_RATIO_LIMIT_PADRAO;
  private static final double UNIT_MATCH_BONUS = 0.12;
  private static final double CONFIANCA_ALTA_LIMIAR = 0.85;

  private final FiscalProductRepository fiscalProductRepository;

  // Default em Java (não só no application.properties) pra continuar correto quando a classe é
  // instanciada direto em teste unitário, sem contexto Spring processando o @Value.
  @Value("${nota.matching.limiar:0.6}")
  private double limiar = 0.6;

  public record Resultado(ProdutoSugerido produtoSugerido, String confianca) {}

  public Resultado buscarMelhorCandidato(
      String produtoLido, String unidadeLida, BigDecimal quantidade) {
    return buscarMelhorCandidato(
        produtoLido, unidadeLida, quantidade, fiscalProductRepository.findAll());
  }

  /**
   * Overload que recebe o catálogo direto, sem tocar no banco — existe pra dar pra testar a lógica
   * de score isoladamente (ver {@code ProdutoMatchingServiceTest}).
   */
  public Resultado buscarMelhorCandidato(
      String produtoLido, String unidadeLida, BigDecimal quantidade, List<FiscalProduct> produtos) {
    if (produtoLido == null || produtoLido.isBlank() || produtos.isEmpty()) {
      return new Resultado(null, "baixa");
    }

    String normLido = normalize(stripNoise(produtoLido));
    if (normLido.isBlank()) {
      return new Resultado(null, "baixa");
    }
    Set<String> tokensLido = tokenize(normLido);
    String unidadeInferida = inferirUnidade(unidadeLida, quantidade);

    FiscalProduct melhor = null;
    double melhorScore = -1;
    for (FiscalProduct produto : produtos) {
      double score = score(tokensLido, normLido, produto, unidadeInferida);
      if (score > melhorScore) {
        melhorScore = score;
        melhor = produto;
      }
    }

    if (melhor == null || melhorScore < limiar) {
      return new Resultado(null, "baixa");
    }

    String confianca = melhorScore >= CONFIANCA_ALTA_LIMIAR ? "alta" : "media";
    ProdutoSugerido sugestao =
        new ProdutoSugerido(
            melhor.getId(), melhor.getCode(), melhor.getDescription(), round(melhorScore));
    return new Resultado(sugestao, confianca);
  }

  private double score(
      Set<String> tokensLido, String normLido, FiscalProduct produto, String unidadeInferida) {
    String normDesc = normalize(produto.getDescription());
    Set<String> tokensDesc = tokenize(normDesc);
    Set<String> nomeTokensDesc =
        tokensDesc.stream().filter(t -> !UNIT_TOKENS.contains(t)).collect(Collectors.toSet());

    long tokensLidoCasados =
        tokensLido.stream().filter(t -> temCorrespondenciaFuzzy(t, nomeTokensDesc)).count();

    double base;
    if (tokensLidoCasados == 0) {
      base = 1 - levenshteinRatio(normLido, normDesc);
    } else {
      double cobertura = (double) tokensLidoCasados / tokensLido.size();
      long tokensDescCasados =
          nomeTokensDesc.stream().filter(t -> temCorrespondenciaFuzzy(t, tokensLido)).count();
      double precisao =
          nomeTokensDesc.isEmpty() ? 1.0 : (double) tokensDescCasados / nomeTokensDesc.size();
      base = (cobertura + precisao) / 2;
    }

    if (unidadeInferida != null) {
      String unidadeProduto = normalizeUnitHint(produto.getUnidadeComercial());
      if (unidadeProduto != null) {
        base += unidadeProduto.equals(unidadeInferida) ? UNIT_MATCH_BONUS : -UNIT_MATCH_BONUS;
      }
    }

    return Math.max(0, Math.min(1, base));
  }

  private boolean temCorrespondenciaFuzzy(String token, Set<String> candidatos) {
    return FuzzyTextMatchUtils.temCorrespondenciaFuzzy(token, candidatos, FUZZY_TOKEN_RATIO_LIMIT);
  }

  /**
   * Unidade explícita do Gemini tem prioridade; na ausência dela, quantidade com parte fracionária
   * "estranha" (ex.: 2.435) é sinal característico de peso pesado em balança — quantidade inteira
   * não entra aqui por ser ambígua demais (pode ser peso arredondado ou contagem de unidades).
   */
  private String inferirUnidade(String unidadeLida, BigDecimal quantidade) {
    if (unidadeLida != null && !unidadeLida.isBlank()) {
      return normalizeUnitHint(unidadeLida);
    }
    if (quantidade != null && quantidade.stripTrailingZeros().scale() > 0) {
      return "KG";
    }
    return null;
  }

  private String normalizeUnitHint(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String norm = normalize(raw);
    if (norm.contains("kg") || norm.contains("quilo")) {
      return "KG";
    }
    if (norm.equals("un") || norm.equals("uni") || norm.equals("und") || norm.contains("unidade")) {
      return "UN";
    }
    if (norm.contains("cx") || norm.contains("caixa")) {
      return "CX";
    }
    if (norm.contains("dz") || norm.contains("duzia")) {
      return "DZ";
    }
    if (norm.contains("fd") || norm.contains("fardo")) {
      return "FD";
    }
    if (norm.contains("pct") || norm.contains("pacote")) {
      return "PCT";
    }
    return raw.toUpperCase(Locale.ROOT);
  }

  private String stripNoise(String text) {
    String semParenteses = PARENTHETICAL.matcher(text).replaceAll(" ");
    return DIGITS.matcher(semParenteses).replaceAll(" ");
  }

  private String normalize(String text) {
    return FuzzyTextMatchUtils.normalize(text);
  }

  private Set<String> tokenize(String normalized) {
    return FuzzyTextMatchUtils.tokenize(normalized);
  }

  private double levenshteinRatio(String a, String b) {
    return FuzzyTextMatchUtils.levenshteinRatio(a, b);
  }

  private double round(double value) {
    return FuzzyTextMatchUtils.round(value);
  }
}
