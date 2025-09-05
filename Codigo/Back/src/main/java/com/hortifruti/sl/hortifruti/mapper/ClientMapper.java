package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.ClientRequest;
import com.hortifruti.sl.hortifruti.dto.ClientResponse;
import com.hortifruti.sl.hortifruti.model.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {

  @Mapping(target = "id", ignore = true)
  Client toClient(ClientRequest clientRequest);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "clientName", target = "clientName")
  @Mapping(source = "email", target = "email")
  @Mapping(source = "phoneNumber", target = "phoneNumber")
  @Mapping(source = "address", target = "address")
  ClientResponse toClientResponse(Client client);
}
