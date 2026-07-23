package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.exception.purchase.PurchaseException;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GroupedProductService {

  public List<GroupedProduct> groupProducts(List<Purchase> purchases, boolean isFixedPrice) {
    if (isFixedPrice) {
      return groupProductsWithFixedPrice(purchases);
    } else {
      return groupProductsWithVariablePrice(purchases);
    }
  }

  private List<GroupedProduct> groupProductsWithFixedPrice(List<Purchase> purchases) {
    if (purchases == null || purchases.isEmpty()) {
      return new ArrayList<>();
    }

    try {
      return new ArrayList<>(
          purchases.stream()
              .flatMap(purchase -> purchase.getInvoiceProducts().stream())
              .collect(
                  Collectors.groupingBy(
                      product -> product.getCode() + "-" + product.getName(),
                      Collectors.collectingAndThen(
                          Collectors.toList(), this::createGroupedProductForFixedPrice)))
              .values());
    } catch (Exception e) {
      throw new PurchaseException("Erro ao agrupar produtos com preço fixo: " + e.getMessage(), e);
    }
  }

  private List<GroupedProduct> groupProductsWithVariablePrice(List<Purchase> purchases) {
    if (purchases == null || purchases.isEmpty()) {
      return new ArrayList<>();
    }

    try {
      Map<String, List<InvoiceProduct>> groupedByCodeAndName =
          purchases.stream()
              .flatMap(purchase -> purchase.getInvoiceProducts().stream())
              .collect(Collectors.groupingBy(product -> extractProductKey(product)));

      return groupedByCodeAndName.entrySet().stream()
          .map(entry -> createGroupedProductForVariablePrice(entry.getKey(), entry.getValue()))
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new PurchaseException(
          "Erro ao agrupar produtos com preço variável: " + e.getMessage(), e);
    }
  }

  private String extractProductKey(InvoiceProduct product) {
    try {
      return product.getCode() + "-" + product.getName();
    } catch (Exception e) {
      throw new PurchaseException("Formato de código de produto inválido: " + product.getCode(), e);
    }
  }

  private GroupedProduct createGroupedProductForFixedPrice(List<InvoiceProduct> productList) {
    if (productList == null || productList.isEmpty()) {
      throw new PurchaseException(
          "Lista de produtos vazia ao tentar agrupar produtos com preço fixo");
    }

    InvoiceProduct firstProduct = productList.get(0);

    BigDecimal totalQuantity =
        productList.stream()
            .map(InvoiceProduct::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal price = firstProduct.getPrice();

    BigDecimal totalValue = price.multiply(totalQuantity);

    return GroupedProduct.builder()
        .code(firstProduct.getCode())
        .name(firstProduct.getName())
        .price(price)
        .quantity(totalQuantity)
        .totalValue(totalValue)
        .build();
  }

  private GroupedProduct createGroupedProductForVariablePrice(
      String key, List<InvoiceProduct> productList) {
    if (productList == null || productList.isEmpty()) {
      throw new PurchaseException(
          "Lista de produtos vazia ao tentar agrupar produtos com preço variável");
    }

    BigDecimal totalValue = BigDecimal.ZERO;
    BigDecimal totalQuantityDecimal = BigDecimal.ZERO;

    InvoiceProduct firstProduct = productList.get(0);

    for (InvoiceProduct product : productList) {
      BigDecimal quantity = product.getQuantity();
      BigDecimal price = product.getPrice();
      BigDecimal productTotal = price.multiply(quantity);

      totalQuantityDecimal = totalQuantityDecimal.add(quantity);
      totalValue = totalValue.add(productTotal);
    }

    BigDecimal weightedAvgPrice;
    try {
      if (totalQuantityDecimal.compareTo(BigDecimal.ZERO) == 0) {
        weightedAvgPrice = BigDecimal.ZERO;
      } else {
        weightedAvgPrice = totalValue.divide(totalQuantityDecimal, 10, RoundingMode.HALF_UP);
      }
    } catch (ArithmeticException e) {
      throw new PurchaseException("Erro ao calcular preço médio ponderado: " + e.getMessage(), e);
    }

    BigDecimal calculatedTotal = weightedAvgPrice.multiply(totalQuantityDecimal);
    if (calculatedTotal.subtract(totalValue).abs().compareTo(new BigDecimal("0.01")) > 0) {
      weightedAvgPrice = totalValue.divide(totalQuantityDecimal, 12, RoundingMode.HALF_UP);
    }

    return GroupedProduct.builder()
        .code(firstProduct.getCode())
        .name(firstProduct.getName())
        .price(weightedAvgPrice)
        .quantity(totalQuantityDecimal)
        .totalValue(totalValue)
        .build();
  }
}
