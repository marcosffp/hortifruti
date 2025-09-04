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

  // Novo método para atualizar diretamente do Request
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "hash", ignore = true)
  void updateTransactionFromRequest(@MappingTarget Transaction target, TransactionRequest source);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "hash", ignore = true)
  Transaction toTransaction(
      String transactionDate,
      String document,
      String history,
      BigDecimal amount,
      String statement,
      TransactionType transactionType,
      Category category);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "hash", ignore = true)
  Transaction toTransaction(TransactionRequest transactionRequest);
}
