package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.dto.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.TransactionResponse;
import com.hortifruti.sl.hortifruti.exception.TransactionException;
import com.hortifruti.sl.hortifruti.mapper.TransactionMapper;
import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.repository.TransactionRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TransactionProcessingService {

  private static final String SICOOB = "sicoob";
  private static final String BB = "bb";

  private final TransactionSicoobService transactionSicoobService;
  private final TransactionBBService transactionBBService;
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final NotificationService notificationService;

  @Async
  public void processFilesAsync(List<MultipartFile> files) {
    try {
      importStatements(files);
      notificationService.sendNotification("Processamento concluído com sucesso.");
    } catch (Exception e) {
      notificationService.sendNotification("Erro no processamento: " + e.getMessage());
    }
  }

  /** Importa extratos de uma lista de arquivos fornecidos. */
  private void importStatements(List<MultipartFile> files) throws IOException {
    if (files == null || files.isEmpty()) {
      throw new TransactionException("Nenhum arquivo foi fornecido para importação.");
    }

    for (MultipartFile file : files) {
      if (file.isEmpty()) {
        throw new TransactionException("Um dos arquivos fornecidos está vazio.");
      }

      String fileName = file.getOriginalFilename();
      if (fileName == null) {
        throw new TransactionException("Um dos arquivos não possui um nome válido.");
      }

      processFileByType(determineFileType(fileName), file);
    }
  }

  /** Determina o tipo de arquivo com base no nome ou conteúdo. */
  private String determineFileType(String fileName) {
    String normalized = fileName.toLowerCase().replaceAll("[^a-z]", "");
    if (normalized.contains(SICOOB)) {
      return SICOOB;
    } else if (normalized.contains(BB)) {
      return BB;
    } else {
      throw new TransactionException("Tipo de arquivo não reconhecido: " + fileName);
    }
  }

  /** Processa um arquivo com base no tipo especificado. */
  private void processFileByType(String tipo, MultipartFile arquivo) throws IOException {
    switch (tipo) {
      case SICOOB:
        transactionSicoobService.importStatement(arquivo);
        break;
      case BB:
        transactionBBService.importStatement(arquivo);
        break;
      default:
        throw new TransactionException("Tipo de arquivo não suportado: " + tipo);
    }
  }

  /** Calcula a receita total do mês atual. */
  public BigDecimal getTotalRevenueForCurrentMonth() {
    return calculateTotalByTypeAndPeriod(TransactionType.CREDITO);
  }

  /** Calcula as despesas totais do mês atual. */
  public BigDecimal getTotalExpensesForCurrentMonth() {
    return calculateTotalByTypeAndPeriod(TransactionType.DEBITO);
  }

  /** Calcula o saldo total do mês atual. */
  public BigDecimal getTotalBalanceForCurrentMonth() {
    return getTotalRevenueForCurrentMonth().subtract(getTotalExpensesForCurrentMonth());
  }

  /** Calcula o total de transações por tipo e período. */
  private BigDecimal calculateTotalByTypeAndPeriod(TransactionType tipo) {
    LocalDate now = LocalDate.now();
    LocalDate inicioDoMes = now.withDayOfMonth(1);
    LocalDate fimDoMes = now.withDayOfMonth(now.lengthOfMonth());

    return transactionRepository.findAll().stream()
        .filter(
            transacao ->
                transacao.getTransactionDate().isAfter(inicioDoMes.minusDays(1))
                    && transacao.getTransactionDate().isBefore(fimDoMes.plusDays(1))
                    && transacao.getTransactionType() == tipo)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
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
}
