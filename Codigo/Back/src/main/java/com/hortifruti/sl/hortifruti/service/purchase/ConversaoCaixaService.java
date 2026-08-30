package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.ItemNotaExtraido;
import com.hortifruti.sl.hortifruti.dto.purchase.ProdutoSugerido;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Conversão determinística de itens em caixa (CX) pra kg, baseada no peso de referência cadastrado
 * por produto ({@link FiscalProduct#getPesoCaixaKg()}, ver {@code ConversaoCaixaImportService}) —
 * substitui a estimativa que antes era deixada a cargo do Gemini (inconsistente entre chamadas, sem
 * memória entre extrações). Chamado pelo {@code GeminiExtractionService} dentro de {@code
 * enriquecerItem}, depois que o {@code ProdutoMatchingService} já sugeriu o produto (é preciso
 * saber qual produto pra buscar o peso de caixa certo).
 */
@Service
@RequiredArgsConstructor
public class ConversaoCaixaService {

  private static final int ESCALA_KG = 3;
  private static final int ESCALA_PRECO = 2;
  private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private final FiscalProductRepository fiscalProductRepository;

  public record ResultadoConversao(
      BigDecimal quantidadeKg, BigDecimal precoPorKg, boolean estimado) {}

  /**
   * Vazio (sem conversão) quando o item não está em caixa, não tem produto identificado, o produto
   * não tem peso de caixa cadastrado, ou falta a quantidade lida — em qualquer um desses casos o
   * item fica como veio da extração, sem inventar um valor.
   */
  public Optional<ResultadoConversao> converterSeNecessario(
      ItemNotaExtraido item, ProdutoSugerido produtoSugerido) {
    if (produtoSugerido == null || !isCaixa(item.unidade()) || item.quantidade() == null) {
      return Optional.empty();
    }

    BigDecimal pesoCaixaKg =
        fiscalProductRepository
            .findById(produtoSugerido.id())
            .map(FiscalProduct::getPesoCaixaKg)
            .orElse(null);
    if (pesoCaixaKg == null || pesoCaixaKg.signum() <= 0) {
      return Optional.empty();
    }

    BigDecimal quantidadeKg =
        item.quantidade().multiply(pesoCaixaKg).setScale(ESCALA_KG, RoundingMode.HALF_UP);
    BigDecimal precoPorKg =
        item.total() != null && quantidadeKg.signum() > 0
            ? item.total().divide(quantidadeKg, ESCALA_PRECO, RoundingMode.HALF_UP)
            : null;

    return Optional.of(new ResultadoConversao(quantidadeKg, precoPorKg, true));
  }

  private boolean isCaixa(String unidade) {
    if (unidade == null || unidade.isBlank()) {
      return false;
    }
    String semAcento =
        DIACRITICS.matcher(Normalizer.normalize(unidade, Normalizer.Form.NFD)).replaceAll("");
    String norm = semAcento.toLowerCase(Locale.ROOT).trim();
    return norm.equals("cx") || norm.contains("caixa");
  }
}
