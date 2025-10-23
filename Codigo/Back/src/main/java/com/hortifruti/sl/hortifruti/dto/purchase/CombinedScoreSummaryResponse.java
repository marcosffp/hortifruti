package com.hortifruti.sl.hortifruti.dto.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CombinedScoreSummaryResponse(
    String clientName,
    int totalItems,
    BigDecimal totalValue,
    List<CombinedScoreDetails> combinedScores) {

  public record CombinedScoreDetails(
      LocalDate confirmedAt,
      LocalDate dueDate,
      BigDecimal totalValue,
      String status,
      boolean hasBillet,
      boolean hasInvoice) {}
}
