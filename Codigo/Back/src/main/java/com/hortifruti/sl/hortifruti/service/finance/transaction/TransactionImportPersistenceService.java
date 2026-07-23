package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.exception.TransactionException;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import com.hortifruti.sl.hortifruti.util.TransactionUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Persistência compartilhada por todos os importadores de extrato bancário (Sicoob, BB): salva
 * transações novas em lote e, em caso de colisão de chave única, refaz a gravação uma a uma para
 * isolar exatamente qual transação falhou.
 */
@Service
@RequiredArgsConstructor
public class TransactionImportPersistenceService {

  private final TransactionRepository transactionRepository;

  public List<Transaction> filterNewTransactions(List<Transaction> transactions) {
    return TransactionUtil.filterNewTransactions(transactions, transactionRepository);
  }

  public List<Transaction> saveNewTransactions(List<Transaction> transactions) {
    if (transactions.isEmpty()) {
      return transactions;
    }
    try {
      return transactionRepository.saveAll(transactions);
    } catch (DataIntegrityViolationException e) {
      return saveTransactionsIndividually(transactions);
    }
  }

  public BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
    return transactions.stream()
        .filter(t -> t.getTransactionType() == type)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
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
}
