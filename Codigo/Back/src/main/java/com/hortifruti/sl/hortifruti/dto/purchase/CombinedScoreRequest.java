package com.hortifruti.sl.hortifruti.dto.purchase;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CombinedScoreRequest(
    @NotNull Long clientId, List<GroupedProductRequest> groupedProducts) {}
