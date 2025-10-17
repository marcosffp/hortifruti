package com.hortifruti.sl.hortifruti.dto.purchase.client;

public record ClientSummary(
    String clientName, String clientAddress, int totalProducts, double totalValue) {}
