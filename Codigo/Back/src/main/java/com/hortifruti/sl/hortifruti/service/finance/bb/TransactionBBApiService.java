package com.hortifruti.sl.hortifruti.service.finance.bb;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.exception.bb.BBApiException;
import com.hortifruti.sl.hortifruti.mapper.TransactionMapper;
import com.hortifruti.sl.hortifruti.model.finance.Category;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.model.finance.TransactionType;
import com.hortifruti.sl.hortifruti.util.TransactionUtil;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Converte os lançamentos retornados pela API Extratos v2 do BB (JsonNode cru, ver BBExtratoClient)
 * em {@link Transaction}.
 *
 * <p>Só considera lançamentos com {@code indicadorTipoLancamento == "1"} (contabilizado) — descarta
 * lançamentos futuros/em processamento e as linhas de saldo/limite (SA, RA, LE, AP, SD, LD, LC,
 * LU). Dentre os contabilizados, os códigos de histórico "000" (saldo anterior) e "999" (saldo
 * final do período) também são descartados por não representarem movimentação real (eles ainda
 * aparecem no extrato exibido, ver {@link BBExtratoLayoutService}, só não viram {@link
 * Transaction}).
 *
 * <p>Dedupe: usa {@code TextoIdentificadorUnicoTransacao} (campo v2, gerado em D-1 segundo a
 * documentação) como chave de hash quando presente; como esse campo pode não existir ainda para
 * lançamentos do próprio dia, cai para um hash textual (data+valor+histórico) nesse caso.
 */
@Service
@RequiredArgsConstructor
public class TransactionBBApiService {

  private static final Set<String> HISTORICOS_SALDO = Set.of("0", "999");

  private final TransactionMapper transactionMapper;

  public List<Transaction> buildTransactions(List<JsonNode> lancamentos, Statement statement) {
    List<Transaction> transactions = new ArrayList<>();
    for (JsonNode lancamento : lancamentos) {
      if (!isContabilizado(lancamento) || isMarcadorDeSaldo(lancamento)) {
        continue;
      }
      transactions.add(buildTransaction(lancamento, statement));
    }
    return transactions;
  }

  private boolean isContabilizado(JsonNode lancamento) {
    return "1".equals(lancamento.path("indicadorTipoLancamento").asText());
  }

  private boolean isMarcadorDeSaldo(JsonNode lancamento) {
    String historico = lancamento.path("codigoHistorico").asText("").replaceFirst("^0+", "");
    return HISTORICOS_SALDO.contains(historico.isEmpty() ? "0" : historico);
  }

  private Transaction buildTransaction(JsonNode lancamento, Statement statement) {
    LocalDate date = BBExtratoParsingUtil.parseData(lancamento.path("dataLancamento").asText());
    String sinal = lancamento.path("indicadorSinalLancamento").asText();
    BigDecimal amount =
        BBExtratoParsingUtil.parseValorComSinal(sinal, lancamento.path("valorLancamento").asText());
    TransactionType transactionType =
        "D".equals(sinal) ? TransactionType.DEBITO : TransactionType.CREDITO;

    String descricao = lancamento.path("textoDescricaoHistorico").asText("").trim();
    String complemento = lancamento.path("textoInformacaoComplementar").asText("").trim();
    String history = complemento.isEmpty() ? descricao : descricao + "  " + complemento;
    if (history.length() > 500) {
      history = history.substring(0, 497) + "...";
    }

    Category category =
        TransactionUtil.determineCategory(
            history.toLowerCase(), transactionType == TransactionType.DEBITO ? "D" : "C");

    String documento = textOrNull(lancamento, "numeroDocumento");
    String agenciaOrigem = textOrNull(lancamento, "codigoAgenciaOrigem");
    String lote = textOrNull(lancamento, "numeroLote");

    Transaction transaction =
        transactionMapper.toTransaction(
            statement,
            lancamento.path("codigoHistorico").asText(""),
            history,
            amount,
            category,
            transactionType,
            documento,
            agenciaOrigem,
            lote,
            date.toString());

    transaction.setHash(generateHash(lancamento, date, amount, history));
    return transaction;
  }

  private String textOrNull(JsonNode node, String field) {
    String value = node.path(field).asText("");
    return value.isBlank() ? null : value;
  }

  private String generateHash(
      JsonNode lancamento, LocalDate date, BigDecimal amount, String history) {
    String idUnico = lancamento.path("TextoIdentificadorUnicoTransacao").asText("");
    String input =
        !idUnico.isBlank() ? "BB_API:" + idUnico : "BB_API:" + date + ":" + amount + ":" + history;
    return sha256(input);
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new BBApiException("Erro ao gerar hash da transação.", e);
    }
  }
}
