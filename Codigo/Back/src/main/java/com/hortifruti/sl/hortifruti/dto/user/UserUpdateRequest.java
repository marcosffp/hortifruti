package com.hortifruti.sl.hortifruti.dto.user;

import com.hortifruti.sl.hortifruti.model.Role;

public record UserUpdateRequest(String username, String password, String position, Role role) {}
