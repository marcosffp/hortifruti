package com.hortifruti.sl.hortifruti.service.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.config.bb.BBExtratoClient;
import com.hortifruti.sl.hortifruti.dto.bb.BBImportSummary;
import com.hortifruti.sl.hortifruti.exception.TransactionException;
import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.enumeration.StatementOrigin;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.repository.finance.StatementRepository;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import com.hortifruti.sl.hortifruti.service.storage.StorageKeyGenerator;
import com.hortifruti.sl.hortifruti.util.SicoobExtratoFormatUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Orquestra a integração com a API Extratos v2 do Banco do Brasil: busca, geração de PDF/Excel e import. */
@Service
@RequiredArgsConstructor
public class BBStatementService {

  private static final DateTimeFormatter BB_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("ddMMyyyy");

  private final StatementRepository statementRepository;
  private final R2StorageService r2StorageService;
  private final BBExtratoClient bbExtratoClient;
  private final BBExtratoPdfGenerator bbExtratoPdfGenerator;
  private final BBExtratoExcelGenerator bbExtratoExcelGenerator;
  private final TransactionBBApiService transactionBBApiService;
  private final TransactionImportPersistenceService transactionImportPersistenceService;

  @Value("${r2.environment}")
  private String environment;

  @Value("${bb.agencia}")
  private String bbAgencia;

  @Value("${bb.conta}")
  private String bbConta;

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
        transactionImportPersistenceService.filterNewTransactions(transacoes);

    List<Transaction> savedTransactions =
        transactionImportPersistenceService.saveNewTransactions(newTransactions);

    BigDecimal totalEntradas =
        transactionImportPersistenceService.sumByType(savedTransactions, TransactionType.CREDITO);
    BigDecimal totalSaidas =
        transactionImportPersistenceService
            .sumByType(savedTransactions, TransactionType.DEBITO)
            .abs();

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
}
