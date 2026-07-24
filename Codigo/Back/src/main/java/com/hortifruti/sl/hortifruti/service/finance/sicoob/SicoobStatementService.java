package com.hortifruti.sl.hortifruti.service.finance.sicoob;

import com.hortifruti.sl.hortifruti.config.sicoob.SicoobExtratoClient;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoResponse;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobImportSummary;
import com.hortifruti.sl.hortifruti.model.finance.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.StatementOrigin;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.model.finance.TransactionType;
import com.hortifruti.sl.hortifruti.repository.finance.StatementRepository;
import com.hortifruti.sl.hortifruti.service.finance.transaction.TransactionImportPersistenceService;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import com.hortifruti.sl.hortifruti.service.storage.StorageKeyGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Orquestra a integração com a API de extrato do Sicoob: busca, geração de PDF/Excel e import. */
@Service
@RequiredArgsConstructor
public class SicoobStatementService {

  private final StatementRepository statementRepository;
  private final R2StorageService r2StorageService;
  private final SicoobExtratoClient sicoobExtratoClient;
  private final SicoobExtratoPdfGenerator sicoobExtratoPdfGenerator;
  private final SicoobExtratoExcelGenerator sicoobExtratoExcelGenerator;
  private final TransactionSicoobApiService transactionSicoobApiService;
  private final TransactionImportPersistenceService transactionImportPersistenceService;

  @Value("${r2.environment}")
  private String environment;

  @Value("${sicoob.num.conta.corrente}")
  private long numeroContaCorrente;

  /**
   * Consulta a API de conta corrente do Sicoob, gera o PDF do extrato a partir dos dados
   * retornados, guarda o PDF no R2 e salva as transações novas (deduplicadas por transactionId, ver
   * {@link TransactionSicoobApiService}).
   */
  public SicoobImportSummary importFromSicoobApi(
      int mes, int ano, Integer diaInicial, Integer diaFinal) throws IOException {
    YearMonth yearMonth = YearMonth.of(ano, mes);
    LocalDate periodStart = LocalDate.of(ano, mes, diaInicial != null ? diaInicial : 1);
    LocalDate periodEnd =
        LocalDate.of(ano, mes, diaFinal != null ? diaFinal : yearMonth.lengthOfMonth());

    Optional<Statement> existingOpt =
        statementRepository.findTopByBankAndOriginOrderByCreatedAtDesc(
            Bank.SICOOB, StatementOrigin.API);

    if (existingOpt.isPresent()) {
      Statement existing = existingOpt.get();
      if (periodStart.equals(existing.getPeriodStart())
          && periodEnd.equals(existing.getPeriodEnd())) {
        return alreadyProcessedSicoobSummary(existing);
      }
    }

    SicoobExtratoResponse extrato =
        sicoobExtratoClient.getExtrato(
            mes, ano, numeroContaCorrente, periodStart.getDayOfMonth(), periodEnd.getDayOfMonth());

    byte[] pdfBytes =
        sicoobExtratoPdfGenerator.generate(periodStart, periodEnd, numeroContaCorrente, extrato);

    Statement statement = existingOpt.orElseGet(Statement::new);
    String key =
        statement.getObjectKey() != null
            ? statement.getObjectKey()
            : StorageKeyGenerator.generate(
                "extratos", environment, UUID.randomUUID().toString(), "pdf");
    r2StorageService.upload(pdfBytes, key, "application/pdf");

    statement.setName(
        "Extrato Sicoob API "
            + mes
            + "/"
            + ano
            + " ("
            + periodStart.getDayOfMonth()
            + "-"
            + periodEnd.getDayOfMonth()
            + ")");
    statement.setObjectKey(key);
    statement.setBank(Bank.SICOOB);
    statement.setOrigin(StatementOrigin.API);
    statement.setPeriodStart(periodStart);
    statement.setPeriodEnd(periodEnd);
    Statement savedStatement = statementRepository.save(statement);

    List<Transaction> transacoes =
        extrato.transacoes() != null
            ? transactionSicoobApiService.buildTransactions(extrato.transacoes(), savedStatement)
            : List.of();
    int totalFetched = transacoes.size();

    List<Transaction> newTransactions =
        transactionImportPersistenceService.filterNewTransactions(transacoes);

    List<Transaction> savedTransactions =
        transactionImportPersistenceService.saveNewTransactions(newTransactions);

    BigDecimal totalEntradas =
        transactionImportPersistenceService.sumByType(savedTransactions, TransactionType.CREDITO);
    BigDecimal totalSaidas =
        transactionImportPersistenceService
            .sumByType(savedTransactions, TransactionType.DEBITO)
            .abs();

    return new SicoobImportSummary(
        savedStatement.getId(),
        false,
        periodStart,
        periodEnd,
        totalFetched,
        savedTransactions.size(),
        totalFetched - savedTransactions.size(),
        totalEntradas,
        totalSaidas);
  }

  /**
   * Consulta a API do Sicoob e gera o PDF do extrato do período, sem persistir nada (só download).
   */
  public byte[] exportSicoobExtratoPdf(int mes, int ano, Integer diaInicial, Integer diaFinal)
      throws IOException {
    SicoobExtratoResponse extrato =
        sicoobExtratoClient.getExtrato(mes, ano, numeroContaCorrente, diaInicial, diaFinal);
    YearMonth yearMonth = YearMonth.of(ano, mes);
    LocalDate periodoInicio = LocalDate.of(ano, mes, diaInicial != null ? diaInicial : 1);
    LocalDate periodoFim =
        LocalDate.of(ano, mes, diaFinal != null ? diaFinal : yearMonth.lengthOfMonth());
    return sicoobExtratoPdfGenerator.generate(
        periodoInicio, periodoFim, numeroContaCorrente, extrato);
  }

  /**
   * Consulta a API do Sicoob e gera o Excel do extrato do período, sem persistir nada (só
   * download).
   */
  public byte[] exportSicoobExtratoExcel(int mes, int ano, Integer diaInicial, Integer diaFinal)
      throws IOException {
    SicoobExtratoResponse extrato =
        sicoobExtratoClient.getExtrato(mes, ano, numeroContaCorrente, diaInicial, diaFinal);
    return sicoobExtratoExcelGenerator.generate(extrato);
  }

  private SicoobImportSummary alreadyProcessedSicoobSummary(Statement existing) {
    return new SicoobImportSummary(
        existing.getId(),
        true,
        existing.getPeriodStart(),
        existing.getPeriodEnd(),
        0,
        0,
        0,
        BigDecimal.ZERO,
        BigDecimal.ZERO);
  }
}
