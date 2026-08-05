package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code consistente} e {@code itensParaConferir} são calculados pela checagem de consistência
 * (Etapa 4 da spec) a partir da soma dos itens x {@code totalGeral} — ficam {@code null}/vazios
 * antes desse passo rodar (ex.: logo após o parse da resposta crua do Gemini, que não tem esses
 * campos) ou quando {@code totalGeral} veio nulo do Gemini (não dá pra julgar sem ele).
 *
 * <p>Deliberadamente só um construtor (o canônico) — ver comentário em {@link ItemNotaExtraido}
 * sobre por que um segundo construtor aqui quebra a desserialização de record do Jackson.
 */
public record NotaExtracaoResponse(
    String cliente,
    String data,
    List<ItemNotaExtraido> itens,
    BigDecimal totalGeral,
    Boolean consistente,
    List<String> itensParaConferir) {}
