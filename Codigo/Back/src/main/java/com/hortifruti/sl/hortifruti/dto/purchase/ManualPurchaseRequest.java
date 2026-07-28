package com.hortifruti.sl.hortifruti.dto.purchase;

import java.time.LocalDate;
import java.util.List;

public record ManualPurchaseRequest(
    Long clientId, LocalDate purchaseDate, List<ManualPurchaseItemRequest> items) {}
