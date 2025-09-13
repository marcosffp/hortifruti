package com.hortifruti.sl.hortifruti.service.transaction;

import com.hortifruti.sl.hortifruti.dto.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.TransactionResponse;
import com.hortifruti.sl.hortifruti.exception.TransactionException;
import com.hortifruti.sl.hortifruti.mapper.TransactionMapper;
import com.hortifruti.sl.hortifruti.model.Statement;
import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.repository.TransactionRepository;
import com.hortifruti.sl.hortifruti.service.NotificationService;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TransactionProcessingService {

  private final TransactionSicoobService transactionSicoobService;
  private final TransactionBBService transactionBBService;
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final NotificationService notificationService;

  @Async
  public void processFileAsync(MultipartFile file, Statement statement) {
    try {
      importStatement(file, statement);
      notificationService.sendNotification("Processamento concluído com sucesso.");
    } catch (Exception e) {
      notificationService.sendNotification("Erro no processamento: " + e.getMessage());
    }
  }

  /** Importa extrato de um arquivo fornecido. */
  private void importStatement(MultipartFile file, Statement statement) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new TransactionException("Nenhum arquivo foi fornecido para importação.");
    }

    String fileName = file.getOriginalFilename();
    if (fileName == null) {
      throw new TransactionException("O arquivo não possui um nome válido.");
    }

    processFileByType(statement.getBank(), file, statement);
  }

  /** Processa um arquivo com base no tipo especificado. */
  private void processFileByType(Bank bank, MultipartFile arquivo, Statement statement)
      throws IOException {
    switch (bank) {
      case SICOOB:
        transactionSicoobService.importStatement(arquivo, statement);
        break;
      case BANCO_DO_BRASIL:
        transactionBBService.importStatement(arquivo, statement);
        break;
      default:
        throw new TransactionException("Tipo de arquivo não suportado: " + bank.toString());
    }
  }

  /** Calcula a receita total do mês atual. */
  public BigDecimal getTotalRevenueForCurrentMonth() {
    List<Transaction> transacoes = transactionRepository.findTransactionsForCurrentMonth();
    return transacoes.stream()
        .filter(transacao -> transacao.getTransactionType() == TransactionType.CREDITO)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Calcula as despesas totais do mês atual. */
  public BigDecimal getTotalExpensesForCurrentMonth() {
    List<Transaction> transacoes = transactionRepository.findTransactionsForCurrentMonth();
    return transacoes.stream()
        .filter(transacao -> transacao.getTransactionType() == TransactionType.DEBITO)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Calcula o saldo total do mês atual. */
  public BigDecimal getTotalBalanceForCurrentMonth() {
    BigDecimal receita = getTotalRevenueForCurrentMonth();
    BigDecimal despesas = getTotalExpensesForCurrentMonth();
    // quero somar
    return receita.add(despesas);
  }

  /** Retorna todas as transações como DTOs. */
  public List<TransactionResponse> getAllTransactions() {
    return transactionRepository.findAll().stream()
        .map(transactionMapper::toResponse)
        .collect(Collectors.toList());
  }

  /** Atualiza uma transação existente. */
  public TransactionResponse updateTransaction(Long id, TransactionRequest transactionRequest) {
    // Busca a transação existente no banco de dados
    Transaction existingTransaction =
        transactionRepository
            .findById(id)
            .orElseThrow(
                () -> new TransactionException("Transação não encontrada com o ID: " + id));

    // Atualiza os campos da transação existente diretamente do request
    transactionMapper.updateTransactionFromRequest(existingTransaction, transactionRequest);

    // Salva a transação atualizada no banco de dados
    Transaction savedTransaction = transactionRepository.save(existingTransaction);

    // Retorna a resposta mapeada
    return transactionMapper.toResponse(savedTransaction);
  }

  /** Exclui uma transação pelo ID. */
  public void deleteTransaction(Long id) {
    if (!transactionRepository.existsById(id)) {
      throw new TransactionException("Transação não encontrada com o ID: " + id);
    }
    transactionRepository.deleteById(id);
  }

  /**
   * Retorna todas as transações filtradas por critérios de pesquisa, tipo e categoria com
   * paginação.
   */
  public Page<TransactionResponse> getAllTransactions(
      String search, String type, String category, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());

    // Busca as transações paginadas diretamente do repositório
    Page<Transaction> transactionsPage = transactionRepository.findAll(pageable);

    // Filtra os resultados
    List<TransactionResponse> filtered =
        transactionsPage.getContent().stream()
            .map(transactionMapper::toResponse)
            .filter(
                tx -> {
                  boolean matches = true;
                  if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    matches =
                        (tx.history() != null && tx.history().toLowerCase().contains(searchLower))
                            || (tx.category() != null
                                && tx.category().name().toLowerCase().contains(searchLower));
                  }
                  if (matches && type != null && !type.isEmpty()) {
                    try {
                      matches = tx.transactionType() == TransactionType.valueOf(type.toUpperCase());
                    } catch (IllegalArgumentException e) {
                      matches = false;
                    }
                  }
                  if (matches && category != null && !category.isEmpty()) {
                    matches = category.equalsIgnoreCase(tx.category().name());
                  }
                  return matches;
                })
            .collect(Collectors.toList());

    // Retorna a página com os resultados filtrados
    return new PageImpl<>(filtered, pageable, transactionsPage.getTotalElements());
  }

  public List<String> getAllCategories() {
    return transactionRepository.findAllCategories();
  }
}
