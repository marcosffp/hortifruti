package com.hortifruti.sl.hortifruti.dto.purchase;

import java.time.LocalDateTime;

public record CombinedScoreResponse(
    Long id,
    String name,
    LocalDateTime dueDate,
    LocalDateTime confirmedAt,
    LocalDateTime updatedAt) {}
