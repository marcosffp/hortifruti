package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.GroupedProduct;
import com.hortifruti.sl.hortifruti.model.InvoiceProduct;
import com.hortifruti.sl.hortifruti.model.Purchase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductGrouper {

  protected List<GroupedProduct> groupProductsWithFixedPrice(List<Purchase> purchases) {
    return new ArrayList<>(
        purchases.stream()
            .flatMap(purchase -> purchase.getInvoiceProducts().stream())
            .collect(
                Collectors.groupingBy(
                    product -> product.getCode() + "-" + product.getName(),
                    Collectors.collectingAndThen(
                        Collectors.toList(), this::createGroupedProductForFixedPrice)))
            .values());
  }

  public List<GroupedProduct> groupProductsWithVariablePrice(List<Purchase> purchases) {
    Map<String, List<InvoiceProduct>> groupedByCodeAndName =
        purchases.stream()
            .flatMap(purchase -> purchase.getInvoiceProducts().stream())
            .collect(
                Collectors.groupingBy(
                    product -> product.getCode().split("-")[0] + "-" + product.getName()));

    return groupedByCodeAndName.entrySet().stream()
        .map(entry -> createGroupedProductForVariablePrice(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private GroupedProduct createGroupedProductForFixedPrice(List<InvoiceProduct> productList) {
    InvoiceProduct firstProduct = productList.get(0);
    int totalQuantity = productList.stream().mapToInt(InvoiceProduct::getQuantity).sum();
    BigDecimal price = firstProduct.getPrice();
    BigDecimal totalValue = price.multiply(BigDecimal.valueOf(totalQuantity));

    return new GroupedProduct(
        firstProduct.getCode(), firstProduct.getName(), price, totalQuantity, totalValue);
  }

  private GroupedProduct createGroupedProductForVariablePrice(
      String key, List<InvoiceProduct> productList) {
    BigDecimal totalValue = BigDecimal.ZERO;
    BigDecimal totalQuantityDecimal = BigDecimal.ZERO;
    int totalQuantity = 0;

    InvoiceProduct firstProduct = productList.get(0);
    for (InvoiceProduct product : productList) {
      int quantity = product.getQuantity();
      totalQuantity += quantity;

      BigDecimal quantityBD = BigDecimal.valueOf(quantity);
      totalQuantityDecimal = totalQuantityDecimal.add(quantityBD);
      totalValue = totalValue.add(product.getPrice().multiply(quantityBD));
    }
    BigDecimal weightedAvgPrice =
        totalQuantityDecimal.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : totalValue.divide(totalQuantityDecimal, 4, RoundingMode.HALF_EVEN);

    return new GroupedProduct(
        firstProduct.getCode(),
        firstProduct.getName(),
        weightedAvgPrice,
        totalQuantity,
        totalValue);
  }
}
