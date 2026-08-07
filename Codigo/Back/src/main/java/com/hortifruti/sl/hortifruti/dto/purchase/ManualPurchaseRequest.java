package com.hortifruti.sl.hortifruti.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record ManualPurchaseRequest(
    @NotNull(message = "Cliente é obrigatório") Long clientId,
    @NotNull(message = "Data da compra é obrigatória") LocalDate purchaseDate,
    @NotEmpty(message = "A compra precisa ter ao menos um item") @Valid
        List<ManualPurchaseItemRequest> items) {}
