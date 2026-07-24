package com.hortifruti.sl.hortifruti.dto.finance;

import com.hortifruti.sl.hortifruti.model.finance.Bank;
import com.hortifruti.sl.hortifruti.model.finance.StatementOrigin;
import java.time.LocalDateTime;

public record StatementResponse(
    Long id, String name, Bank bank, StatementOrigin origin, LocalDateTime createdAt) {}
