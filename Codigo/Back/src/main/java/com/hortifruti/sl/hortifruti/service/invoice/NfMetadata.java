package com.hortifruti.sl.hortifruti.service.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Metadados fiscais da NF extraídos da resposta da Focus NFe. */
record NfMetadata(String nfNumber, String clientName, BigDecimal totalValue, LocalDate issuedAt) {}
