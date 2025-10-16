package com.hortifruti.sl.hortifruti.dto.transaction;

import java.time.LocalDate;

public record TransactionRequestDate(LocalDate startDate, LocalDate endDate) {}
