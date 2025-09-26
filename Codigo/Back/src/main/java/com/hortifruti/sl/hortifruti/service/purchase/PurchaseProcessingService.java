package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.exception.PurchaseProcessingException;
import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.model.InvoiceProduct;
import com.hortifruti.sl.hortifruti.model.Purchase;
import com.hortifruti.sl.hortifruti.repository.InvoiceProductRepository;
import com.hortifruti.sl.hortifruti.repository.PurchaseRepository;
import com.hortifruti.sl.hortifruti.service.ClientService;
import com.hortifruti.sl.hortifruti.util.PdfUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class PurchaseProcessingService {
  private final ClientService clientService;
  private final PurchaseRepository purchaseRepository;
  private final InvoiceProductRepository invoiceProductRepository;

  @Transactional
  protected Purchase processPurchaseFile(MultipartFile file) throws IOException {
    try {
      String pdfText = PdfUtil.extractPdfText(file);
      String clientName = PdfUtil.findValueByKeyword(pdfText, "CLIENTE");
      LocalDate purchaseDate = parsePurchaseDate(PdfUtil.findValueByKeyword(pdfText, "DATA"));
      List<InvoiceProduct> invoiceProducts = extractProducts(pdfText);
      Client client = clientService.findMatchingClient(clientName);
      BigDecimal total =
          invoiceProducts.stream()
              .filter(product -> product.getQuantity() > 0)
              .map(
                  product -> product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())))
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      Purchase purchase =
          Purchase.builder()
              .client(client)
              .purchaseDate(purchaseDate.atStartOfDay())
              .total(total)
              .build();

      purchase = purchaseRepository.save(purchase);

      List<InvoiceProduct> productsToSave =
          invoiceProducts.stream().filter(product -> product.getQuantity() > 0).toList();

      List<InvoiceProduct> savedProducts = new ArrayList<>();

      for (InvoiceProduct product : productsToSave) {
        // Mantém o código original da nota
        // Associa o produto à compra
        product.setPurchase(purchase);

        // Salva o produto sem alterar seu código
        InvoiceProduct savedProduct = invoiceProductRepository.save(product);
        savedProducts.add(savedProduct);
      }

      purchase.setInvoiceProducts(savedProducts);
      purchase = purchaseRepository.save(purchase);

      return purchase;
    } catch (PurchaseProcessingException e) {
      throw e;
    } catch (IOException e) {
      throw new PurchaseProcessingException("Erro ao processar arquivo PDF: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new PurchaseProcessingException("Erro ao processar compra: " + e.getMessage(), e);
    }
  }

  private LocalDate parsePurchaseDate(String purchaseDate) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
      return LocalDate.parse(purchaseDate, formatter);
    } catch (Exception e) {
      throw new PurchaseProcessingException("Formato de data inválido: " + purchaseDate);
    }
  }

  private List<InvoiceProduct> extractProducts(String text) {
    List<InvoiceProduct> products = new ArrayList<>();
    String[] lines = text.split("\n");

    boolean isProductSection = false;
    for (String line : lines) {
      line = line.trim();

      if (line.contains("COD")
          && line.contains("PRODUTO")
          && line.contains("QUANT")
          && line.contains("KG")) {
        isProductSection = true;
        continue;
      }

      if (!isProductSection) continue;

      if (line.matches("^\\d+\\s+.*")) {
        InvoiceProduct product = parseProductLine(line);
        if (product != null) {
          products.add(product);
        }
      }
    }

    if (products.isEmpty()) {
      throw new PurchaseProcessingException("Nenhum produto encontrado no documento");
    }

    return products;
  }

  private InvoiceProduct parseProductLine(String line) {
    try {
      String[] parts = line.split("\\s+");
      if (parts.length < 2) return null;

      String code = parts[0];
      String name = extractProductName(parts);
      int currentIndex = name.split("\\s+").length + 1;

      Integer quantity = parseQuantity(parts, currentIndex);
      BigDecimal unitPrice = parseUnitPrice(parts, currentIndex + 1);

      return InvoiceProduct.builder()
          .code(code)
          .name(name)
          .quantity(quantity)
          .price(unitPrice)
          .unitType("kg")
          .build();
    } catch (Exception e) {
      return null;
    }
  }

  private String extractProductName(String[] parts) {
    StringBuilder nameBuilder = new StringBuilder();
    int currentIndex = 1;
    while (currentIndex < parts.length
        && !parts[currentIndex].matches("\\d+([,.]\\d+)?")
        && !parts[currentIndex].contains("R$")) {
      nameBuilder.append(parts[currentIndex]).append(" ");
      currentIndex++;
    }
    return nameBuilder.toString().trim();
  }

  private Integer parseQuantity(String[] parts, int index) {
    if (index < parts.length && parts[index].matches("\\d+")) {
      return Integer.parseInt(parts[index]);
    }
    return 0;
  }

  private BigDecimal parseUnitPrice(String[] parts, int index) {
    if (index < parts.length && parts[index].matches("\\d+([,.]\\d+)?")) {
      return new BigDecimal(parts[index].replace(",", "."));
    }
    return BigDecimal.ZERO;
  }
}
