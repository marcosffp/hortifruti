package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.model.Statement;
import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementSelectionService {

  private final StatementRepository statementRepository;

  /**
   * Busca o melhor statement que cobre o período solicitado para cada banco
   * Prioriza statements que tenham transações completas do mês,
   * mas se não houver, pega o que melhor cobre o período
   */
  public List<Statement> getBestStatementsForMonth(int month, int year) {
    List<Statement> bestStatements = new ArrayList<>();
    
    // Calcular o período do mês
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDate startOfMonth = yearMonth.atDay(1);
    LocalDate endOfMonth = yearMonth.atEndOfMonth();
    
    log.info("Buscando statements para o período: {} a {}", startOfMonth, endOfMonth);

    // Buscar para Banco do Brasil
    Optional<Statement> bbStatement = getBestStatementForPeriod(Bank.BANCO_DO_BRASIL, startOfMonth, endOfMonth);
    bbStatement.ifPresent(statement -> {
      log.info("Statement BB selecionado: {} (ID: {})", statement.getName(), statement.getId());
      bestStatements.add(statement);
    });

    // Buscar para Sicoob
    Optional<Statement> sicoobStatement = getBestStatementForPeriod(Bank.SICOOB, startOfMonth, endOfMonth);
    sicoobStatement.ifPresent(statement -> {
      log.info("Statement Sicoob selecionado: {} (ID: {})", statement.getName(), statement.getId());
      bestStatements.add(statement);
    });

    log.info("Total de statements selecionados: {}", bestStatements.size());
    return bestStatements;
  }

  /**
   * Busca o melhor statement para um banco específico no período
   */
  private Optional<Statement> getBestStatementForPeriod(Bank bank, LocalDate startDate, LocalDate endDate) {
    // Estratégia em 3 etapas:
    // 1. Tentar encontrar statements que tenham transações exatamente no período
    // 2. Se não encontrar, buscar statements que tenham pelo menos algumas transações no período
    // 3. Se ainda não encontrar, pegar o statement mais recente do banco

    // Etapa 1: Buscar statements com melhor cobertura do período
    List<Statement> bestCoverageStatements = statementRepository.findBestCoverageStatementsForPeriod(
        bank, startDate, endDate);
    
    if (!bestCoverageStatements.isEmpty()) {
      Statement bestStatement = bestCoverageStatements.get(0);
      int transactionsInPeriod = countTransactionsInPeriod(bestStatement, startDate, endDate);
      
      log.info("Encontrado statement com melhor cobertura para {}: {} transações no período", 
               bank, transactionsInPeriod);
      
      return Optional.of(bestStatement);
    }

    // Etapa 2: Buscar qualquer statement que tenha transações no período
    List<Statement> statementsWithTransactions = statementRepository.findStatementsWithTransactionsInPeriod(
        bank, startDate, endDate);
    
    if (!statementsWithTransactions.isEmpty()) {
      Statement statement = statementsWithTransactions.get(0);
      int transactionsInPeriod = countTransactionsInPeriod(statement, startDate, endDate);
      
      log.info("Encontrado statement com algumas transações para {}: {} transações no período", 
               bank, transactionsInPeriod);
      
      return Optional.of(statement);
    }

    // Etapa 3: Pegar o statement mais recente como fallback
    Optional<Statement> recentStatement = statementRepository.findTopByBankOrderByCreatedAtDesc(bank);
    
    if (recentStatement.isPresent()) {
      log.warn("Nenhum statement com transações no período encontrado para {}. " +
               "Usando o mais recente como fallback: {}", 
               bank, recentStatement.get().getName());
      
      return recentStatement;
    }

    log.warn("Nenhum statement encontrado para o banco: {}", bank);
    return Optional.empty();
  }

  /**
   * Conta quantas transações existem no período especificado
   */
  private int countTransactionsInPeriod(Statement statement, LocalDate startDate, LocalDate endDate) {
    if (statement.getTransactions() == null) {
      return 0;
    }

    return (int) statement.getTransactions().stream()
        .filter(transaction -> {
          LocalDate transactionDate = transaction.getTransactionDate();
          return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
        })
        .count();
  }

  /**
   * Filtra as transações de um statement para um período específico
   */
  public List<Transaction> getTransactionsForPeriod(Statement statement, LocalDate startDate, LocalDate endDate) {
    if (statement.getTransactions() == null) {
      return new ArrayList<>();
    }

    return statement.getTransactions().stream()
        .filter(transaction -> {
          LocalDate transactionDate = transaction.getTransactionDate();
          return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
        })
        .collect(Collectors.toList());
  }

  /**
   * Fornece estatísticas sobre a cobertura dos statements
   */
  public String getStatementCoverageInfo(List<Statement> statements, int month, int year) {
    StringBuilder info = new StringBuilder();
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDate startOfMonth = yearMonth.atDay(1);
    LocalDate endOfMonth = yearMonth.atEndOfMonth();

    info.append("Relatório de Cobertura dos Statements:\n");
    info.append("Período solicitado: ").append(startOfMonth).append(" a ").append(endOfMonth).append("\n\n");

    for (Statement statement : statements) {
      int transactionsInPeriod = countTransactionsInPeriod(statement, startOfMonth, endOfMonth);
      int totalTransactions = statement.getTransactions() != null ? statement.getTransactions().size() : 0;
      
      double coveragePercentage = totalTransactions > 0 ? 
          (double) transactionsInPeriod / totalTransactions * 100 : 0;

      info.append("Statement: ").append(statement.getName()).append("\n");
      info.append("Banco: ").append(statement.getBank()).append("\n");
      info.append("Data de criação: ").append(statement.getCreatedAt().toLocalDate()).append("\n");
      info.append("Transações no período: ").append(transactionsInPeriod).append("\n");
      info.append("Total de transações: ").append(totalTransactions).append("\n");
      info.append("Cobertura: ").append(String.format("%.1f%%", coveragePercentage)).append("\n\n");
    }

    return info.toString();
  }
}