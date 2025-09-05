package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.TransactionResponse;
import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

  TransactionResponse toResponse(Transaction transaction);

  void updateTransaction(@MappingTarget Transaction target, Transaction source);

  // Atualiza a entidade Transaction a partir do TransactionRequest
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "hash", ignore = true)
  void updateTransactionFromRequest(@MappingTarget Transaction target, TransactionRequest source);

  // Cria uma nova Transaction a partir de TransactionRequest
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "hash", ignore = true)
  Transaction toTransaction(TransactionRequest transactionRequest);

  // Método para criar uma Transaction com parâmetros individuais
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "hash", ignore = true)
  Transaction toTransaction(
      String statement,
      String codHistory,
      String history,
      BigDecimal amount,
      Category category,
      TransactionType transactionType,
      String document,
      String sourceAgency,
      String batch,
      String transactionDate);
}
