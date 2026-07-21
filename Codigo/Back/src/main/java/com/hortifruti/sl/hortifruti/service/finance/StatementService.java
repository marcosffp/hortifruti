package com.hortifruti.sl.hortifruti.service.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.config.bb.BBExtratoClient;
import com.hortifruti.sl.hortifruti.config.sicoob.SicoobExtratoClient;
import com.hortifruti.sl.hortifruti.dto.bb.BBImportSummary;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoResponse;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobImportSummary;
import com.hortifruti.sl.hortifruti.dto.transaction.StatementResponse;
import com.hortifruti.sl.hortifruti.exception.TransactionException;
import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.enumeration.StatementOrigin;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.repository.finance.StatementRepository;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import com.hortifruti.sl.hortifruti.service.storage.StorageKeyGenerator;
import com.hortifruti.sl.hortifruti.util.SicoobExtratoFormatUtil;
import com.hortifruti.sl.hortifruti.util.TransactionUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatementService {
  private final StatementRepository statementRepository;
  private final TransactionRepository transactionRepository;
  private final R2StorageService r2StorageService;
  private final SicoobExtratoClient sicoobExtratoClient;
  private final SicoobExtratoPdfGenerator sicoobExtratoPdfGenerator;
  private final SicoobExtratoExcelGenerator sicoobExtratoExcelGenerator;
  private final TransactionSicoobApiService transactionSicoobApiService;
  private final BBExtratoClient bbExtratoClient;
  private final BBExtratoPdfGenerator bbExtratoPdfGenerator;
  private final BBExtratoExcelGenerator bbExtratoExcelGenerator;
  private final TransactionBBApiService transactionBBApiService;

  private static final DateTimeFormatter BB_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("ddMMyyyy");

  @Value("${r2.environment}")
  private String environment;

  @Value("${sicoob.num.conta.corrente}")
  private long numeroContaCorrente;

  @Value("${bb.agencia}")
  private String bbAgencia;

  @Value("${bb.conta}")
  private String bbConta;

  public List<StatementResponse> listAll() {
    return statementRepository.findAll().stream()
        .map(
            s ->
                new StatementResponse(
                    s.getId(), s.getName(), s.getBank(), s.getOrigin(), s.getCreatedAt()))
        .collect(Collectors.toList());
  }

  /**
   * Lê o conteúdo do arquivo do extrato, do R2 (registros novos) ou da coluna legada (registros
   * salvos antes da migração para R2).
   */
  public byte[] getFileContent(Statement statement) {
    if (statement.getObjectKey() != null) {
      return r2StorageService.download(statement.getObjectKey());
    }
    return statement.getFilePath();
  }

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
        TransactionUtil.filterNewTransactions(transacoes, transactionRepository);

    List<Transaction> savedTransactions = saveNewTransactions(newTransactions);

    BigDecimal totalEntradas = sumByType(savedTransactions, TransactionType.CREDITO);
    BigDecimal totalSaidas = sumByType(savedTransactions, TransactionType.DEBITO).abs();

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

  /**
   * Consulta a API Extratos v2 do BB (percorrendo todas as páginas do período), gera o PDF do
   * extrato a partir dos lançamentos retornados, guarda o PDF no R2 e salva as transações novas
   * (deduplicadas por TextoIdentificadorUnicoTransacao, com fallback textual — ver {@link
   * TransactionBBApiService}).
   */
  public BBImportSummary importFromBBApi(LocalDate dataInicio, LocalDate dataFim)
      throws IOException {
    if (dataInicio.getMonthValue() != dataFim.getMonthValue()
        || dataInicio.getYear() != dataFim.getYear()) {
      throw new TransactionException(
          "O período de busca do extrato do BB precisa estar dentro do mesmo mês.");
    }

    Optional<Statement> existingOpt =
        statementRepository.findTopByBankAndOriginOrderByCreatedAtDesc(
            Bank.BANCO_DO_BRASIL, StatementOrigin.API);

    if (existingOpt.isPresent()) {
      Statement existing = existingOpt.get();
      if (dataInicio.equals(existing.getPeriodStart()) && dataFim.equals(existing.getPeriodEnd())) {
        return alreadyProcessedBBSummary(existing);
      }
    }

    List<JsonNode> lancamentos =
        bbExtratoClient.getExtratoPeriodo(formatBBDate(dataInicio), formatBBDate(dataFim));

    byte[] pdfBytes =
        bbExtratoPdfGenerator.generate(dataInicio, dataFim, bbAgencia, bbConta, lancamentos);

    Statement statement = existingOpt.orElseGet(Statement::new);
    String key =
        statement.getObjectKey() != null
            ? statement.getObjectKey()
            : StorageKeyGenerator.generate(
                "extratos", environment, UUID.randomUUID().toString(), "pdf");
    r2StorageService.upload(pdfBytes, key, "application/pdf");

    statement.setName(
        "Extrato BB API "
            + dataInicio.format(SicoobExtratoFormatUtil.DATA_LONGA)
            + " a "
            + dataFim.format(SicoobExtratoFormatUtil.DATA_LONGA));
    statement.setObjectKey(key);
    statement.setBank(Bank.BANCO_DO_BRASIL);
    statement.setOrigin(StatementOrigin.API);
    statement.setPeriodStart(dataInicio);
    statement.setPeriodEnd(dataFim);
    Statement savedStatement = statementRepository.save(statement);

    List<Transaction> transacoes =
        transactionBBApiService.buildTransactions(lancamentos, savedStatement);
    int totalFetched = transacoes.size();

    List<Transaction> newTransactions =
        TransactionUtil.filterNewTransactions(transacoes, transactionRepository);

    List<Transaction> savedTransactions = saveNewTransactions(newTransactions);

    BigDecimal totalEntradas = sumByType(savedTransactions, TransactionType.CREDITO);
    BigDecimal totalSaidas = sumByType(savedTransactions, TransactionType.DEBITO).abs();

    return new BBImportSummary(
        savedStatement.getId(),
        false,
        dataInicio,
        dataFim,
        totalFetched,
        savedTransactions.size(),
        totalFetched - savedTransactions.size(),
        totalEntradas,
        totalSaidas);
  }

  private BBImportSummary alreadyProcessedBBSummary(Statement existing) {
    return new BBImportSummary(
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

  /** Consulta a API do BB e gera o PDF do extrato do período, sem persistir nada (só download). */
  public byte[] exportBBExtratoPdf(LocalDate dataInicio, LocalDate dataFim) throws IOException {
    List<JsonNode> lancamentos =
        bbExtratoClient.getExtratoPeriodo(formatBBDate(dataInicio), formatBBDate(dataFim));
    return bbExtratoPdfGenerator.generate(dataInicio, dataFim, bbAgencia, bbConta, lancamentos);
  }

  /**
   * Consulta a API do BB e gera o Excel do extrato do período, sem persistir nada (só download).
   */
  public byte[] exportBBExtratoExcel(LocalDate dataInicio, LocalDate dataFim) throws IOException {
    List<JsonNode> lancamentos =
        bbExtratoClient.getExtratoPeriodo(formatBBDate(dataInicio), formatBBDate(dataFim));
    return bbExtratoExcelGenerator.generate(lancamentos);
  }

  private String formatBBDate(LocalDate date) {
    return date.format(BB_DATE_FORMATTER);
  }

  private List<Transaction> saveNewTransactions(List<Transaction> transactions) {
    if (transactions.isEmpty()) {
      return transactions;
    }
    try {
      return transactionRepository.saveAll(transactions);
    } catch (DataIntegrityViolationException e) {
      return saveTransactionsIndividually(transactions);
    }
  }

  private List<Transaction> saveTransactionsIndividually(List<Transaction> transactions) {
    List<Transaction> savedTransactions = new ArrayList<>();
    for (int i = 0; i < transactions.size(); i++) {
      try {
        Transaction transaction = transactions.get(i);
        transaction.setId(null);
        savedTransactions.add(transactionRepository.save(transaction));
      } catch (Exception e) {
        throw new TransactionException(
            "Erro ao salvar transação " + (i + 1) + " do extrato via API: " + e.getMessage(), e);
      }
    }
    return savedTransactions;
  }

  private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
    return transactions.stream()
        .filter(t -> t.getTransactionType() == type)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
