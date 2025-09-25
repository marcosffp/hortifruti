package com.hortifruti.sl.hortifruti.dto.user;

import com.hortifruti.sl.hortifruti.model.enumeration.Role;

public record UserResponse(Long id, String username, String position, Role role) {}
