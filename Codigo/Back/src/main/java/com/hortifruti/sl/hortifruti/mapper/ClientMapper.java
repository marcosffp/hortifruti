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
  Client toClient(ClientRequest clientRequest);

  // totalPurchaseValue não vem mais da entidade (contador mutável que dessincronizava) —
  // é preenchido em ClientService a partir da soma real das compras do cliente.
  @Mapping(target = "totalPurchaseValue", ignore = true)
  ClientResponse toClientResponse(Client client);
}
