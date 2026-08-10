package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.finance.NewTransactionData;
import com.hortifruti.sl.hortifruti.dto.finance.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.finance.TransactionResponse;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

  @Mapping(target = "bank", source = "transaction.statement.bank")
  @Mapping(target = "origin", source = "transaction.statement.origin")
  TransactionResponse toResponse(Transaction transaction);

  void updateTransaction(@MappingTarget Transaction target, Transaction source);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "hash", ignore = true)
  void updateTransactionFromRequest(@MappingTarget Transaction target, TransactionRequest source);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "hash", ignore = true)
  Transaction toTransaction(NewTransactionData data);
}
