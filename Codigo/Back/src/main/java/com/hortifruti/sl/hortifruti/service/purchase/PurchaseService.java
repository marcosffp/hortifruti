package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProduct;
import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProductsResponse;
import com.hortifruti.sl.hortifruti.exception.ClientException;
import com.hortifruti.sl.hortifruti.exception.PurchaseException;
import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.model.Purchase;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.PurchaseRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class PurchaseService {

  private final PurchaseProcessingService purchaseProcessingService;
  private final PurchaseRepository purchaseRepository;
  private final ClientRepository clientRepository;
  private final ProductGrouper productGrouper;

  public Purchase processPurchaseFile(MultipartFile file) throws IOException {
    if (file == null) {
      throw new PurchaseException("Arquivo não fornecido");
    }
    return purchaseProcessingService.processPurchaseFile(file);
  }

  @Transactional(readOnly = true)
  public GroupedProductsResponse getGroupedProductsByClientAndPeriod(
      Long clientId, LocalDateTime startDate, LocalDateTime endDate) {

    if (clientId == null) {
      throw new ClientException("ID do cliente não fornecido");
    }

    if (startDate == null || endDate == null) {
      throw new PurchaseException("Período de consulta inválido: datas não podem ser nulas");
    }

    if (endDate.isBefore(startDate)) {
      throw new PurchaseException("Data final não pode ser anterior à data inicial");
    }

    Client client =
        clientRepository
            .findById(clientId)
            .orElseThrow(
                () -> new ClientException("Cliente com ID " + clientId + " não encontrado"));

    List<Purchase> purchases =
        purchaseRepository.findByClientIdAndPurchaseDateBetween(clientId, startDate, endDate);

    if (purchases.isEmpty()) {
      return new GroupedProductsResponse(client.getClientName(), 0, BigDecimal.ZERO, List.of());
    }

    List<GroupedProduct> groupedProducts =
        client.isVariablePrice()
            ? productGrouper.groupProductsWithVariablePrice(purchases)
            : productGrouper.groupProductsWithFixedPrice(purchases);

    BigDecimal totalValue =
        groupedProducts.stream()
            .map(GroupedProduct::totalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new GroupedProductsResponse(
        client.getClientName(), groupedProducts.size(), totalValue, groupedProducts);
  }

  public void deletePurchaseById(Long id) {
    Purchase purchase =
        purchaseRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Compra não encontrada com o ID: " + id));

    purchaseRepository.delete(purchase);
  }
}
