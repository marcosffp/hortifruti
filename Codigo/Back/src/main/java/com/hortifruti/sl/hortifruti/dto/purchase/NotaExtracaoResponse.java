package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code consistente} e {@code itensParaConferir} são calculados pela checagem de consistência
 * (Etapa 4 da spec) a partir da soma dos itens x {@code totalGeral} — ficam {@code null}/vazios
 * antes desse passo rodar (ex.: logo após o parse da resposta crua do Gemini, que não tem esses
 * campos) ou quando {@code totalGeral} veio nulo do Gemini (não dá pra julgar sem ele).
 *
 * <p>{@code clienteSugerido}/{@code clienteConfianca} seguem o mesmo padrão do {@code
 * produtoSugerido}/{@code confianca} de {@link ItemNotaExtraido}: ficam {@code null} logo após o
 * parse da resposta crua do Gemini, e são preenchidos depois pelo {@code ClienteMatchingService}.
 *
 * <p>{@code itensComDivergenciaPreco} (nomes de produto cujo preço lido diverge do preço oficial da
 * tabela de preços do cliente — mesmo formato de {@code itensParaConferir}) e {@code
 * semTabelaPrecoParaCompetencia} (nenhuma tabela {@code CONFIRMADA} cobre a data da nota, pro
 * cliente sugerido) vêm do cross-check contra a {@code TabelaPrecoCliente} — ver {@code
 * NotaPrecoOficialChecker}. Ambos ficam {@code null}/vazios quando o cliente da nota ainda não foi
 * identificado.
 *
 * <p>Diferente de {@link ItemNotaExtraido}, este record NÃO vem direto do JSON do Gemini (é
 * inteiramente montado no backend em {@code GeminiExtractionService#enriquecerNota}), então não tem
 * a mesma restrição de wither/construtor único — um construtor normal no fim do fluxo é seguro.
 */
public record NotaExtracaoResponse(
    String cliente,
    String data,
    List<ItemNotaExtraido> itens,
    BigDecimal totalGeral,
    Boolean consistente,
    List<String> itensParaConferir,
    ClienteSugerido clienteSugerido,
    String clienteConfianca,
    List<String> itensComDivergenciaPreco,
    Boolean semTabelaPrecoParaCompetencia) {}
