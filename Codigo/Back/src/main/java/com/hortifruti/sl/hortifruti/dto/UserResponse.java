package com.hortifruti.sl.hortifruti.dto;

import com.hortifruti.sl.hortifruti.model.enumeration.Role;

public record UserResponse(Long id, String username, Role role) {}
