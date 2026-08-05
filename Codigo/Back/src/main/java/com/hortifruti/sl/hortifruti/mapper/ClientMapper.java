package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientResponse;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(source = "stateRegistration", target = "stateRegistration")
  @Mapping(source = "stateIndicator", target = "stateIndicator")
  @Mapping(source = "cideCode", target = "cideCode")
  @Mapping(source = "onlyBillet", target = "onlyBillet")
  @Mapping(source = "requiresPurchaseProof", target = "requiresPurchaseProof")
  Client toClient(ClientRequest clientRequest);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "clientName", target = "clientName")
  @Mapping(source = "email", target = "email")
  @Mapping(source = "phoneNumber", target = "phoneNumber")
  @Mapping(source = "address", target = "address")
  @Mapping(source = "variablePrice", target = "variablePrice")
  @Mapping(source = "document", target = "document")
  @Mapping(source = "cideCode", target = "cideCode")
  @Mapping(source = "stateRegistration", target = "stateRegistration")
  @Mapping(source = "stateIndicator", target = "stateIndicator")
  @Mapping(source = "onlyBillet", target = "onlyBillet")
  @Mapping(source = "requiresPurchaseProof", target = "requiresPurchaseProof")
  @Mapping(source = "lastPurchaseDate", target = "lastPurchaseDate")
  // totalPurchaseValue não vem mais da entidade (contador mutável que dessincronizava) —
  // é preenchido em ClientService a partir da soma real das compras do cliente.
  @Mapping(target = "totalPurchaseValue", ignore = true)
  ClientResponse toClientResponse(Client client);
}
