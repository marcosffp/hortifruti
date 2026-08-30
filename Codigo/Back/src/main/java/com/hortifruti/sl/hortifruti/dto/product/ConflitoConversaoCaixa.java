package com.hortifruti.sl.hortifruti.dto.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mesmo código de produto aparece mais de uma vez no arquivo com pesos de caixa diferentes (ex.:
 * código 146 como 18kg e 15kg no mesmo import). Nunca sobrescreve silenciosamente um valor por
 * outro: aplica o primeiro valor encontrado no arquivo ({@code valorAplicado}) e sinaliza o
 * conflito pro usuário revisar/corrigir a planilha.
 */
public record ConflitoConversaoCaixa(
    String codigo, List<BigDecimal> valoresEncontrados, BigDecimal valorAplicado) {}
