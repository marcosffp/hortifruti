package com.hortifruti.sl.hortifruti.service.finance.transaction;

import com.hortifruti.sl.hortifruti.dto.finance.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.finance.TransactionRequestDate;
import com.hortifruti.sl.hortifruti.dto.finance.TransactionResponse;
import com.hortifruti.sl.hortifruti.exception.finance.TransactionException;
import com.hortifruti.sl.hortifruti.mapper.TransactionMapper;
import com.hortifruti.sl.hortifruti.model.finance.Category;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.model.finance.TransactionType;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionProcessingService {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  @Transactional(readOnly = true)
  public List<TransactionResponse> getAllTransactions() {
    return transactionRepository.findAll().stream()
        .map(transactionMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
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

  @Transactional(readOnly = true)
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
                          criteriaBuilder.lower(root.get("category").as(String.class)),
                          searchPattern)));
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
      try {
        Category categoryEnum = Category.valueOf(category.toUpperCase());
        spec =
            spec.and(
                (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("category"), categoryEnum));
      } catch (IllegalArgumentException e) {
        throw new TransactionException("Categoria inválida: " + category, e);
      }
    }

    Page<Transaction> transactionsPage = transactionRepository.findAll(spec, pageable);

    return transactionsPage.map(transactionMapper::toResponse);
  }

  public List<String> getAllCategories() {
    return transactionRepository.findAllCategories().stream().map(Enum::name).toList();
  }

  public List<Transaction> findTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
    return transactionRepository.findAll(
        TransactionSpecifications.transactionDateBetween(startDate, endDate));
  }

  public List<Transaction> findTransactionsByCreatedAtBetween(
      LocalDateTime startDate, LocalDateTime endDate) {
    return transactionRepository.findAll(
        TransactionSpecifications.createdAtBetween(startDate, endDate));
  }

  public void deleteAllByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
    transactionRepository.deleteAll(
        transactionRepository.findAll(
            TransactionSpecifications.createdAtBetween(startDate, endDate)));
  }

  public BigDecimal getTotalRevenue(TransactionRequestDate request) {
    LocalDate startDate = request.startDate();
    LocalDate endDate = request.endDate();

    if (startDate == null || endDate == null) {
      startDate = LocalDate.now().withDayOfMonth(1);
      endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
    }
    List<Transaction> transacoes = findTransactionsByDateRange(startDate, endDate);
    return transacoes.stream()
        .filter(transacao -> transacao.getTransactionType() == TransactionType.CREDITO)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public BigDecimal getTotalExpenses(TransactionRequestDate request) {
    LocalDate startDate = request.startDate();
    LocalDate endDate = request.endDate();

    if (startDate == null || endDate == null) {
      startDate = LocalDate.now().withDayOfMonth(1);
      endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
    }
    List<Transaction> transacoes = findTransactionsByDateRange(startDate, endDate);
    return transacoes.stream()
        .filter(transacao -> transacao.getTransactionType() == TransactionType.DEBITO)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public BigDecimal getTotalBalance(TransactionRequestDate request) {
    BigDecimal receita = getTotalRevenue(request);
    BigDecimal despesas = getTotalExpenses(request);
    return receita.subtract(despesas.abs());
  }
}
