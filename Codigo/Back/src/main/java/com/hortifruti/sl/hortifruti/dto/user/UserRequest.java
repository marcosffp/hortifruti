package com.hortifruti.sl.hortifruti.dto.user;

import com.hortifruti.sl.hortifruti.model.enumeration.Role;

public record UserRequest(String username, String password, String position, Role role) {}
