package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.GroupedProduct;
import com.hortifruti.sl.hortifruti.dto.GroupedProductsResponse;
import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.model.Purchase;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.PurchaseRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class PurchaseService {

  private static final Logger logger = LoggerFactory.getLogger(PurchaseService.class);

  private final PurchaseProcessingService purchaseProcessingService;
  private final PurchaseRepository purchaseRepository;
  private final ClientRepository clientRepository;
  private final ProductGrouper productGrouper;

  public Purchase processPurchaseFile(MultipartFile file) throws IOException {
    return purchaseProcessingService.processPurchaseFile(file);
  }

  @Transactional(readOnly = true)
  public GroupedProductsResponse getGroupedProductsByClientAndPeriod(
      Long clientId, LocalDateTime startDate, LocalDateTime endDate) {
    logger.info(
        "Buscando compras para cliente ID {} no período de {} até {}",
        clientId,
        startDate,
        endDate);

    Client client =
        clientRepository
            .findById(clientId)
            .orElseThrow(
                () -> {
                  logger.error("Cliente com ID {} não encontrado", clientId);
                  return new RuntimeException("Cliente não encontrado");
                });

    List<Purchase> purchases =
        purchaseRepository.findByClientIdAndPurchaseDateBetween(clientId, startDate, endDate);
    logger.info("Encontradas {} compras no período", purchases.size());

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
}
