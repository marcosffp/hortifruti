package com.hortifruti.sl.hortifruti.dto.product;

import java.util.List;

/**
 * Resumo estruturado de um import de conversão caixa→kg ({@code ConversaoCaixaImportService}) —
 * permite revisar visualmente o que aconteceu a cada import sem precisar ir no banco conferir.
 */
public record ConversaoCaixaImportResponse(
    List<ProdutoConversaoCadastrado> cadastrados,
    List<ProdutoConversaoAtualizado> atualizados,
    List<String> semAlteracao,
    List<String> codigosNaoEncontrados,
    List<ConflitoConversaoCaixa> conflitosNoArquivo) {}
