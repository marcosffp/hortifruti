package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.ItemNotaExtraido;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Checagem de consistência aritmética da nota extraída (Etapa 4 da spec) — não depende de mais IA,
 * é só matemática: sinaliza item cuja conta interna (quantidade × preço) não bate com o total da
 * linha, e sinaliza a nota inteira quando a soma dos itens não bate com o total geral lido.
 */
public final class NotaConsistenciaChecker {

  private static final int ITENS_PARA_CONFERIR_LIMITE = 3;

  private NotaConsistenciaChecker() {}

  /**
   * {@code true} quando quantidade × preço bate com o total da linha (dentro da margem), ou quando
   * algum dos três campos é {@code null} (sem dado suficiente pra julgar, não penaliza por campo
   * ilegível).
   */
  public static boolean itemConsistente(ItemNotaExtraido item, BigDecimal margem) {
    if (item.quantidade() == null || item.precoUnitario() == null || item.total() == null) {
      return true;
    }
    BigDecimal calculado = item.quantidade().multiply(item.precoUnitario());
    return calculado.subtract(item.total()).abs().compareTo(margem) <= 0;
  }

  public static boolean notaConsistente(
      BigDecimal totalCalculado, BigDecimal totalGeral, BigDecimal margem) {
    return totalCalculado.subtract(totalGeral).abs().compareTo(margem) <= 0;
  }

  public static BigDecimal somaItens(List<ItemNotaExtraido> itens) {
    return itens.stream()
        .map(ItemNotaExtraido::total)
        .filter(java.util.Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Prioriza itens cuja própria conta interna (quantidade × preço) já prova estar errada — é uma
   * causa concreta da diferença, não um palpite — e só completa a lista com os de maior valor
   * (palpite: são os que mais pesam na diferença, então os primeiros a conferir) quando sobra
   * espaço.
   */
  public static List<String> itensParaConferir(List<ItemNotaExtraido> itens, BigDecimal margem) {
    List<String> inconsistentes =
        itens.stream()
            .filter(item -> !itemConsistente(item, margem))
            .map(ItemNotaExtraido::produtoLido)
            .toList();

    List<String> porValor =
        itens.stream()
            .filter(item -> itemConsistente(item, margem))
            .sorted(
                Comparator.comparing(
                        (ItemNotaExtraido item) ->
                            item.total() == null ? BigDecimal.ZERO : item.total())
                    .reversed())
            .map(ItemNotaExtraido::produtoLido)
            .toList();

    return Stream.concat(inconsistentes.stream(), porValor.stream())
        .distinct()
        .limit(ITENS_PARA_CONFERIR_LIMITE)
        .toList();
  }
}
