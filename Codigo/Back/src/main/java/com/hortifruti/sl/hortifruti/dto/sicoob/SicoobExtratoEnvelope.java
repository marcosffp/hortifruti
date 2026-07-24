package com.hortifruti.sl.hortifruti.dto.sicoob;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Envelope real da resposta 200 de {@code GET /conta-corrente/v4/extrato/{mes}/{ano}} do Sicoob: os
 * dados do extrato vêm dentro de {@code resultado}, não na raiz do JSON (a
 * documentacao_sicoob_api.md não deixa isso claro — confirmado com uma resposta real). {@code
 * mensagens} não é usado hoje, mantido como {@link JsonNode} para não travar em formatos
 * inesperados.
 */
public record SicoobExtratoEnvelope(JsonNode mensagens, SicoobExtratoResponse resultado) {}
