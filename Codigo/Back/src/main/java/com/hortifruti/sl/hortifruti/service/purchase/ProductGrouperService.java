package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.exception.PurchaseException;
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
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProductGrouperService {

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
      return product.getCode().split("-")[0] + "-" + product.getName();
    } catch (Exception e) {
      throw new PurchaseException("Formato de código de produto inválido: " + product.getCode(), e);
    }
  }

  private GroupedProduct createGroupedProductForFixedPrice(List<InvoiceProduct> productList) {
    if (productList == null || productList.isEmpty()) {
      throw new PurchaseException(
          "Lista de produtos vazia ao tentar agrupar produtos com preço fixo");
    }

    // Obtém o primeiro produto da lista
    InvoiceProduct firstProduct = productList.get(0);

    // Calcula a quantidade total
    int totalQuantity = productList.stream().mapToInt(InvoiceProduct::getQuantity).sum();

    // Obtém o preço do primeiro produto
    BigDecimal price = firstProduct.getPrice();

    // Calcula o valor total
    BigDecimal totalValue = price.multiply(BigDecimal.valueOf(totalQuantity));

    // Cria e retorna o GroupedProduct (transient - não persiste aqui)
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

    // Inicializa os valores totais
    BigDecimal totalValue = BigDecimal.ZERO;
    BigDecimal totalQuantityDecimal = BigDecimal.ZERO;
    int totalQuantity = 0;

    // Obtém o primeiro produto da lista
    InvoiceProduct firstProduct = productList.get(0);

    // Calcula os totais de quantidade e valor
    for (InvoiceProduct product : productList) {
      int quantity = product.getQuantity();
      totalQuantity += quantity;

      BigDecimal quantityBD = BigDecimal.valueOf(quantity);
      totalQuantityDecimal = totalQuantityDecimal.add(quantityBD);
      totalValue = totalValue.add(product.getPrice().multiply(quantityBD));
    }

    // Calcula o preço médio ponderado
    BigDecimal weightedAvgPrice;
    try {
      weightedAvgPrice =
          totalQuantityDecimal.compareTo(BigDecimal.ZERO) == 0
              ? BigDecimal.ZERO
              : totalValue.divide(totalQuantityDecimal, 4, RoundingMode.HALF_EVEN);
    } catch (ArithmeticException e) {
      throw new PurchaseException("Erro ao calcular preço médio ponderado: " + e.getMessage(), e);
    }

    // Cria e retorna o GroupedProduct (transient - não persiste aqui)
    return GroupedProduct.builder()
        .code(firstProduct.getCode())
        .name(firstProduct.getName())
        .price(weightedAvgPrice)
        .quantity(totalQuantity)
        .totalValue(totalValue)
        .build();
  }
}
