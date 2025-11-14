package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.dto.transaction.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.transaction.TransactionRequestDate;
import com.hortifruti.sl.hortifruti.dto.transaction.TransactionResponse;
import com.hortifruti.sl.hortifruti.exception.TransactionException;
import com.hortifruti.sl.hortifruti.mapper.TransactionMapper;
import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

  @Async
  public void processFileAsync(MultipartFile file, Statement statement) {
    try {
      if (statement.getBank() == Bank.SICOOB) {
        importStatement(file, statement);
      } else {
        importStatement(file, statement);
      }
    } catch (Exception e) {
      throw new TransactionException("Erro ao processar o arquivo: " + e.getMessage(), e);
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

  /** Retorna todas as transações como DTOs. */
  public List<TransactionResponse> getAllTransactions() {
    return transactionRepository.findAll().stream()
        .map(transactionMapper::toResponse)
        .collect(Collectors.toList());
  }

  /** Atualiza uma transação existente. */
  public TransactionResponse updateTransaction(Long id, TransactionRequest transactionRequest) {
    Transaction existingTransaction =
        transactionRepository
            .findById(id)
            .orElseThrow(
                () -> new TransactionException("Transação não encontrada com o ID: " + id));

    transactionMapper.updateTransactionFromRequest(existingTransaction, transactionRequest);

    Transaction savedTransaction = transactionRepository.save(existingTransaction);

    return transactionMapper.toResponse(savedTransaction);
  }

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

    Specification<Transaction> spec = Specification.allOf();

    if (search != null && !search.isEmpty()) {
      String searchPattern = "%" + search.toLowerCase() + "%";
      spec =
          spec.and(
              (root, query, criteriaBuilder) ->
                  criteriaBuilder.or(
                      criteriaBuilder.like(
                          criteriaBuilder.lower(root.get("history")), searchPattern),
                      criteriaBuilder.like(
                          criteriaBuilder.lower(root.get("category")), searchPattern)));
    }

    if (type != null && !type.isEmpty()) {
      try {
        TransactionType transactionType = TransactionType.valueOf(type.toUpperCase());
        spec =
            spec.and(
                (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("transactionType"), transactionType));
      } catch (IllegalArgumentException e) {
        throw new TransactionException("Tipo de transação inválido: " + type, e);
      }
    }

    if (category != null && !category.isEmpty()) {
      spec =
          spec.and(
              (root, query, criteriaBuilder) ->
                  criteriaBuilder.equal(
                      criteriaBuilder.lower(root.get("category")), category.toLowerCase()));
    }

    Page<Transaction> transactionsPage = transactionRepository.findAll(spec, pageable);

    return transactionsPage.map(transactionMapper::toResponse);
  }

  public List<String> getAllCategories() {
    return transactionRepository.findAllCategories();
  }

  /**
   * Calcula a receita total para um período especificado ou para o mês atual se não for informado.
   */
  public BigDecimal getTotalRevenue(TransactionRequestDate request) {
    LocalDate startDate = request.startDate();
    LocalDate endDate = request.endDate();

    if (startDate == null || endDate == null) {
      startDate = LocalDate.now().withDayOfMonth(1);
      endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
    }
    List<Transaction> transacoes =
        transactionRepository.findTransactionsByDateRange(startDate, endDate);
    return transacoes.stream()
        .filter(transacao -> transacao.getTransactionType() == TransactionType.CREDITO)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Calcula as despesas totais para um período especificado ou para o mês atual se não for
   * informado.
   */
  public BigDecimal getTotalExpenses(TransactionRequestDate request) {
    LocalDate startDate = request.startDate();
    LocalDate endDate = request.endDate();

    if (startDate == null || endDate == null) {
      startDate = LocalDate.now().withDayOfMonth(1);
      endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
    }
    List<Transaction> transacoes =
        transactionRepository.findTransactionsByDateRange(startDate, endDate);
    return transacoes.stream()
        .filter(transacao -> transacao.getTransactionType() == TransactionType.DEBITO)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Calcula o saldo total para um período especificado ou para o mês atual se não for informado.
   */
  public BigDecimal getTotalBalance(TransactionRequestDate request) {
    BigDecimal receita = getTotalRevenue(request);
    BigDecimal despesas = getTotalExpenses(request);
    return receita.subtract(despesas.abs());
  }
}
