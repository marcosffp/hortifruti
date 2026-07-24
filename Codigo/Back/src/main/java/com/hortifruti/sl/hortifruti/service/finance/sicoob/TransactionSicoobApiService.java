package com.hortifruti.sl.hortifruti.service.finance.sicoob;

import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoTransacao;
import com.hortifruti.sl.hortifruti.exception.sicoob.SicoobExtratoException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Converte as transações retornadas pela API de conta corrente do Sicoob (JSON estruturado) em
 * {@link Transaction}. O hash de deduplicação usa o {@code transactionId} que a própria API
 * retorna, o que torna reconsultas de um período que se sobrepõe (ex.: 1–20 hoje, 1–21 amanhã)
 * idempotentes: {@link TransactionUtil#filterNewTransactions} já descarta automaticamente qualquer
 * transação cujo hash já exista.
 */
@Service
@RequiredArgsConstructor
public class TransactionSicoobApiService {

  private final TransactionMapper transactionMapper;

  public List<Transaction> buildTransactions(
      List<SicoobExtratoTransacao> transacoes, Statement statement) {
    List<Transaction> transactions = new ArrayList<>();
    for (SicoobExtratoTransacao transacao : transacoes) {
      transactions.add(buildTransaction(transacao, statement));
    }
    return transactions;
  }

  private Transaction buildTransaction(SicoobExtratoTransacao transacao, Statement statement) {
    LocalDate date = SicoobExtratoParsingUtil.parseData(transacao.data());
    boolean credito = SicoobExtratoParsingUtil.isCredito(transacao.tipo(), transacao.valor());
    TransactionType transactionType = credito ? TransactionType.CREDITO : TransactionType.DEBITO;
    BigDecimal amount =
        SicoobExtratoParsingUtil.parseValorComSinal(transacao.tipo(), transacao.valor());

    String history = transacao.descricao() != null ? transacao.descricao().trim() : "";
    if (history.length() > 500) {
      history = history.substring(0, 497) + "...";
    }

    Category category =
        TransactionUtil.determineCategory(history.toLowerCase(), credito ? "C" : "D");

    Transaction transaction =
        transactionMapper.toTransaction(
            statement,
            "",
            history,
            amount,
            category,
            transactionType,
            transacao.numeroDocumento(),
            "",
            "",
            date.toString());

    transaction.setHash(
        generateApiTransactionHash(transacao.transactionId(), date, amount, history));
    return transaction;
  }

  private String generateApiTransactionHash(
      String transactionId, LocalDate date, BigDecimal amount, String history) {
    String input =
        (transactionId != null && !transactionId.isBlank())
            ? "SICOOB_API:" + transactionId
            : "SICOOB_API:" + date + ":" + amount + ":" + history;
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
      throw new SicoobExtratoException("Erro ao gerar hash da transação.", e);
    }
  }
}
