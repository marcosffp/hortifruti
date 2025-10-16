package com.hortifruti.sl.hortifruti.dto.transaction;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import java.time.LocalDateTime;

public record StatementResponse(Long id, String name, Bank bank, LocalDateTime createdAt) {}
