package com.hortifruti.sl.hortifruti.service.invoice.factory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hortifruti.sl.hortifruti.dto.invoice.ItemRequest;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import com.hortifruti.sl.hortifruti.service.invoice.ProductNFService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvoiceItem {
    private final ProductNFService productService;
    private final String PIS="01";
    private final String COFINS="01";
    private final String ICMS_ORIGEM="0"; 

    public List<ItemRequest> createItems(List<GroupedProduct> groupedProducts) {
        List<ItemRequest> items = new ArrayList<>();
        for (GroupedProduct product : groupedProducts) {
            Map<String, Object> productData = productService.findProductByCode(product.getCode());

            ItemRequest item = new ItemRequest(
                    product.getCode(),
                    product.getName(),
                    (String) productData.get("ncm"),
                    (String) productData.get("cfop"),
                    (String) productData.get("unidade_comercial"),
                    new BigDecimal(product.getQuantity()),
                    product.getPrice(),
                    product.getTotalValue(),
                    (String) productData.get("unidade_tributavel"),
                    new BigDecimal(product.getQuantity()), 
                    product.getPrice(), 
                    (String) productData.get("icms"), 
                    ICMS_ORIGEM,
                    PIS,
                    COFINS
            );
            items.add(item);
        }
        return items;
    }
}
