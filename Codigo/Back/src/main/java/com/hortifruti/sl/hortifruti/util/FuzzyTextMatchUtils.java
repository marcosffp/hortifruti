package com.hortifruti.sl.hortifruti.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Normalização/tokenização/fuzzy-match de texto genérico, extraído de {@code
 * service.purchase.ProdutoMatchingService} e {@code service.purchase.ClienteMatchingService} —
 * ambos cruzam texto lido (OCR ou arquivo de cliente) contra um cadastro do sistema pra sugerir o
 * item certo, e implementavam a mesma lógica de normalização/Levenshtein duas vezes. Um terceiro
 * consumidor (matching de produto do cliente LLinea x catálogo interno) tornou a duplicação
 * genuinamente cross-módulo, por isso vive aqui em vez de em {@code service.purchase}.
 */
public final class FuzzyTextMatchUtils {

  private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
  private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  /** Limiar de razão de Levenshtein usado pelos matchers existentes antes dessa extração. */
  public static final double FUZZY_TOKEN_RATIO_LIMIT_PADRAO = 0.34;

  private FuzzyTextMatchUtils() {}

  public static String normalize(String text) {
    String semAcento =
        DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
    return semAcento.toLowerCase(Locale.ROOT).trim();
  }

  public static Set<String> tokenize(String normalized) {
    return NON_ALNUM
        .splitAsStream(normalized)
        .filter(t -> !t.isBlank())
        .collect(Collectors.toSet());
  }

  public static int levenshtein(String a, String b) {
    int m = a.length();
    int n = b.length();
    if (m == 0) {
      return n;
    }
    if (n == 0) {
      return m;
    }

    int[] dp = new int[n + 1];
    for (int j = 0; j <= n; j++) {
      dp[j] = j;
    }
    for (int i = 1; i <= m; i++) {
      int prev = dp[0];
      dp[0] = i;
      for (int j = 1; j <= n; j++) {
        int temp = dp[j];
        dp[j] =
            a.charAt(i - 1) == b.charAt(j - 1)
                ? prev
                : 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
        prev = temp;
      }
    }
    return dp[n];
  }

  public static double levenshteinRatio(String a, String b) {
    int maxLen = Math.max(a.length(), b.length());
    if (maxLen == 0) {
      return 0;
    }
    return (double) levenshtein(a, b) / maxLen;
  }

  /** Igual, prefixo mútuo (tamanho ≥ 3) ou razão de Levenshtein dentro do limiar. */
  public static boolean tokensEquivalentes(String a, String b, double fuzzyRatioLimit) {
    if (a.equals(b)) {
      return true;
    }
    if (a.length() >= 3 && b.length() >= 3 && (a.startsWith(b) || b.startsWith(a))) {
      return true;
    }
    return levenshteinRatio(a, b) <= fuzzyRatioLimit;
  }

  public static boolean temCorrespondenciaFuzzy(
      String token, Set<String> candidatos, double fuzzyRatioLimit) {
    return candidatos.stream()
        .anyMatch(candidato -> tokensEquivalentes(token, candidato, fuzzyRatioLimit));
  }

  public static double round(double value) {
    return Math.round(value * 100) / 100.0;
  }
}
