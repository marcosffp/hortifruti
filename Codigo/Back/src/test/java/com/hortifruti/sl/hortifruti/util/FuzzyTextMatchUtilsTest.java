package com.hortifruti.sl.hortifruti.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FuzzyTextMatchUtilsTest {

  @Test
  void normalizeRemoveAcentoEBaixaCaixa() {
    assertThat(FuzzyTextMatchUtils.normalize("Couve-Flôr AÇÚCAR")).isEqualTo("couve-flor acucar");
  }

  @Test
  void tokenizeSeparaPorNaoAlfanumerico() {
    assertThat(FuzzyTextMatchUtils.tokenize("alface roxa-kg"))
        .containsExactlyInAnyOrder("alface", "roxa", "kg");
  }

  @Test
  void levenshteinRatioZeroParaStringsIguais() {
    assertThat(FuzzyTextMatchUtils.levenshteinRatio("alface", "alface")).isZero();
  }

  @Test
  void tokensEquivalentesPorPrefixoMutuo() {
    assertThat(FuzzyTextMatchUtils.tokensEquivalentes("cebol", "cebola", 0.34)).isTrue();
  }

  @Test
  void tokensEquivalentesPorErroDeDigitacao() {
    assertThat(FuzzyTextMatchUtils.tokensEquivalentes("lotecao", "lotacao", 0.34)).isTrue();
  }

  @Test
  void tokensNaoEquivalentesQuandoMuitoDiferentes() {
    assertThat(FuzzyTextMatchUtils.tokensEquivalentes("maca", "cebola", 0.34)).isFalse();
  }

  @Test
  void temCorrespondenciaFuzzyEncontraCandidatoEquivalente() {
    assertThat(
            FuzzyTextMatchUtils.temCorrespondenciaFuzzy(
                "alface", Set.of("cenoura", "alfaces"), 0.34))
        .isTrue();
  }
}
