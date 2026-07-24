package com.hortifruti.sl.hortifruti.service.finance.bb;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.dto.bb.BBExtratoLinha;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Monta as linhas de exibição do extrato do BB (usadas pelo PDF e pelo Excel). Diferente do Sicoob
 * (que só mostra saldo por dia, ver {@link SicoobExtratoLayoutService}), o extrato original do BB
 * mostra o saldo acumulado após cada lançamento — calculado aqui somando o valor de cada lançamento
 * na ordem em que a API os retorna, já que a API não devolve esse saldo por linha, só a lista de
 * lançamentos.
 */
@Service
public class BBExtratoLayoutService {

  public List<BBExtratoLinha> montarLinhas(List<JsonNode> lancamentos) {
    List<BBExtratoLinha> linhas = new ArrayList<>();
    BigDecimal saldo = BigDecimal.ZERO;

    for (JsonNode lancamento : lancamentos) {
      if (!"1".equals(lancamento.path("indicadorTipoLancamento").asText())) {
        continue;
      }

      String sinal = lancamento.path("indicadorSinalLancamento").asText();
      BigDecimal valor =
          BBExtratoParsingUtil.parseValorComSinal(
              sinal, lancamento.path("valorLancamento").asText());
      saldo = saldo.add(valor);

      String historicoCodigo = lancamento.path("codigoHistorico").asText("");
      String descricao = lancamento.path("textoDescricaoHistorico").asText("").trim();
      String historico = historicoCodigo.isBlank() ? descricao : historicoCodigo + " " + descricao;
      boolean destaque = descricao.toUpperCase().contains("SALDO");

      linhas.add(
          new BBExtratoLinha(
              BBExtratoParsingUtil.parseData(lancamento.path("dataLancamento").asText()),
              textOrNull(lancamento, "codigoAgenciaOrigem"),
              textOrNull(lancamento, "numeroLote"),
              textOrNull(lancamento, "numeroDocumento"),
              historico,
              valor,
              saldo,
              destaque));

      String complemento = lancamento.path("textoInformacaoComplementar").asText("").trim();
      if (!complemento.isEmpty()) {
        linhas.add(new BBExtratoLinha(null, null, null, null, complemento, null, null, false));
      }
    }

    return linhas;
  }

  private String textOrNull(JsonNode node, String field) {
    String value = node.path(field).asText("");
    return value.isBlank() ? null : value;
  }
}
