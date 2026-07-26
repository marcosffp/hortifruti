package com.hortifruti.sl.hortifruti.dto.user;

import java.util.List;

public record AuthUserResponse(
    Long id, String username, String name, List<String> roles, String environment) {}
