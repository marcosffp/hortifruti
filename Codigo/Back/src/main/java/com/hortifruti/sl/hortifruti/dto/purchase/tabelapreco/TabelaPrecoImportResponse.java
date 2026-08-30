package com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco;

import java.util.List;

/**
 * Resumo estruturado de um import de tabela de preços de cliente ({@code
 * TabelaPrecoClienteImportService}) — mesmo espírito do {@code ConversaoCaixaImportResponse}:
 * permite revisar visualmente o que aconteceu a cada import sem ir no banco conferir. {@code
 * precosEmBrancoNoArquivo} é só informativo (célula VRUNI vazia = não cotado esse mês, não é erro).
 */
public record TabelaPrecoImportResponse(
    Long tabelaPrecoClienteId,
    List<ItemAutoAplicado> autoAplicadosPorMapeamento,
    List<ItemSugerido> sugeridosAltaConfianca,
    List<ItemSugerido> sugeridosBaixaConfianca,
    List<ItemSemCorrespondencia> semCorrespondencia,
    int precosEmBrancoNoArquivo) {}
