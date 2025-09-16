package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.UserRequest;
import com.hortifruti.sl.hortifruti.dto.UserResponse;
import com.hortifruti.sl.hortifruti.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  User toUser(UserRequest userRequest);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "position", target = "position")
  @Mapping(source = "role", target = "role")
  UserResponse toUserResponse(User user);
}
