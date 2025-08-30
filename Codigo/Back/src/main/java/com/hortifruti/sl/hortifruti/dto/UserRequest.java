package com.hortifruti.sl.hortifruti.dto;

import com.hortifruti.sl.hortifruti.models.enumeration.Role;

public record UserRequest(String username, String password, Role role) {}
