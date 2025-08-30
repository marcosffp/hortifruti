package com.hortifruti.sl.hortifruti.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.hortifruti.sl.hortifruti.dto.UserRequest;
import com.hortifruti.sl.hortifruti.dto.UserResponse;
import com.hortifruti.sl.hortifruti.models.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  User toUser(UserRequest userRequest);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "role", target = "role")
  UserResponse toUserResponse(User user);
}