package com.hortifruti.sl.hortifruti.dto.transaction;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.enumeration.StatementOrigin;
import java.time.LocalDateTime;

public record StatementResponse(
    Long id, String name, Bank bank, StatementOrigin origin, LocalDateTime createdAt) {}
