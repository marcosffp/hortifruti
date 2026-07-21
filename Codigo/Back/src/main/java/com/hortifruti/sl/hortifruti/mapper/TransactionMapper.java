package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.transaction.TransactionRequest;
import com.hortifruti.sl.hortifruti.dto.transaction.TransactionResponse;
import com.hortifruti.sl.hortifruti.model.enumeration.Category;
import com.hortifruti.sl.hortifruti.model.enumeration.TransactionType;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import java.math.BigDecimal;
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
  Transaction toTransaction(
      Statement statement,
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
