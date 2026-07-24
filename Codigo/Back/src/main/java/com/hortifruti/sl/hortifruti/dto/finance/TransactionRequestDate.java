package com.hortifruti.sl.hortifruti.dto.finance;

import java.time.LocalDate;

public record TransactionRequestDate(LocalDate startDate, LocalDate endDate) {}
