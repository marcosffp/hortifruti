package com.hortifruti.sl.hortifruti.service.invoice.factory;

import com.hortifruti.sl.hortifruti.dto.invoice.ItemRequest;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import com.hortifruti.sl.hortifruti.service.invoice.ProductNFService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceItem {
  private final ProductNFService productService;
  private final String PIS = "01";
  private final String COFINS = "01";
  private final String ICMS_ORIGEM = "0";

  public List<ItemRequest> createItems(
      List<GroupedProduct> groupedProducts, String ufDestinatario) {
    List<ItemRequest> items = new ArrayList<>();
    for (GroupedProduct product : groupedProducts) {
      Map<String, Object> productData = productService.findProductByCode(product.getCode());
      String cfop = (String) productData.get("cfop");

      // Se destinatário for de outro estado, troca série 5xxx → 6xxx
      if (cfop != null && cfop.startsWith("5") && !"MG".equalsIgnoreCase(ufDestinatario)) {
        cfop = "6" + cfop.substring(1);
      }
      ItemRequest item =
          new ItemRequest(
              product.getCode(),
              product.getName(),
              (String) productData.get("ncm"),
              cfop,
              (String) productData.get("unidade_comercial"),
              product.getQuantity(),
              product.getPrice(),
              product.getTotalValue(),
              (String) productData.get("unidade_tributavel"),
              product.getQuantity(),
              product.getPrice(),
              (String) productData.get("icms"),
              ICMS_ORIGEM,
              PIS,
              COFINS);
      items.add(item);
    }
    return items;
  }
}
