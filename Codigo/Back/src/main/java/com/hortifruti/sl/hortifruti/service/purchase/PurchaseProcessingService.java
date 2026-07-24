package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.exception.purchase.PurchaseException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.purchase.InvoiceProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
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
  public Purchase processPurchaseFile(MultipartFile file) throws IOException {
    try {
      if (file == null || file.isEmpty()) {
        throw new PurchaseException("O arquivo enviado está vazio.");
      }

      String pdfText = PdfUtil.extractPdfText(file);
      if (pdfText == null || pdfText.isBlank()) {
        throw new PurchaseException("O conteúdo do arquivo PDF está vazio ou inválido.");
      }

      String clientName = PdfUtil.findValueByKeyword(pdfText, "CTE");

      if (clientName == null || clientName.isBlank()) {
        throw new PurchaseException("O nome do cliente não foi encontrado no arquivo.");
      }

      String purchaseDateString = PdfUtil.findValueByKeyword(pdfText, "DATA");

      if (purchaseDateString == null || purchaseDateString.isBlank()) {
        throw new PurchaseException("A data da compra não foi encontrada no arquivo.");
      }

      LocalDate purchaseDate = parsePurchaseDate(purchaseDateString);
      List<InvoiceProduct> invoiceProducts = extractProducts(pdfText);

      Client client = clientService.findMatchingClient(clientName);
      if (client == null) {
        throw new PurchaseException("Nenhum cliente correspondente foi encontrado.");
      }

      BigDecimal total =
          invoiceProducts.stream()
              .filter(product -> product.getQuantity().compareTo(BigDecimal.ZERO) > 0)
              .map(product -> product.getPrice().multiply(product.getQuantity()))
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (total.compareTo(BigDecimal.ZERO) <= 0) {
        throw new PurchaseException("O total da compra não pode ser zero ou negativo.");
      }

      Purchase purchase =
          Purchase.builder()
              .client(client)
              .purchaseDate(purchaseDate.atStartOfDay())
              .total(total)
              .build();

      purchase = purchaseRepository.save(purchase);

      List<InvoiceProduct> savedProducts = new ArrayList<>();

      for (InvoiceProduct product : invoiceProducts) {
        if (product.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
          product.setPurchase(purchase);
          InvoiceProduct savedProduct = invoiceProductRepository.save(product);
          savedProducts.add(savedProduct);
        }
      }

      purchase.setInvoiceProducts(savedProducts);
      purchase = purchaseRepository.save(purchase);

      return purchase;
    } catch (PurchaseException e) {
      throw e;
    } catch (IOException e) {
      throw new PurchaseException("Erro ao processar arquivo PDF: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new PurchaseException("Erro inesperado ao processar a compra: " + e.getMessage(), e);
    }
  }

  private LocalDate parsePurchaseDate(String purchaseDate) {
    try {
      String cleanDate = purchaseDate.trim();

      if (cleanDate.contains("TOTAL:")) {
        cleanDate = cleanDate.substring(0, cleanDate.indexOf("TOTAL:")).trim();
      }

      if (cleanDate.contains("R$")) {
        cleanDate = cleanDate.substring(0, cleanDate.indexOf("R$")).trim();
      }

      cleanDate = cleanDate.replaceAll("\\s+", "");

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
      return LocalDate.parse(cleanDate, formatter);
    } catch (Exception e) {
      throw new PurchaseException("Formato de data inválido: " + purchaseDate);
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
          && line.contains("QTD")
          && line.contains("KG")) {
        isProductSection = true;
        continue;
      }

      if (!isProductSection) continue;

      if (line.matches("^\\d+\\s+.*")) {
        if (hasValidQuantity(line)) {
          InvoiceProduct product = parseProductLine(line);
          if (product != null) {
            products.add(product);
          }
        }
      }
    }

    if (products.isEmpty()) {
      throw new PurchaseException("Nenhum produto encontrado no documento.");
    }

    return products;
  }

  private boolean hasValidQuantity(String line) {
    try {
      String[] parts = line.split("\\s+");

      if (parts.length < 3) return false;

      String name = extractProductName(parts);
      int quantityIndex = name.split("\\s+").length + 1;

      if (quantityIndex < parts.length - 1) {
        String quantityStr = parts[quantityIndex];
        return !quantityStr.trim().isEmpty()
            && quantityStr.matches("\\d+([,.]\\d+)?")
            && !quantityStr.contains("R$");
      }

      return false;
    } catch (Exception e) {
      return false;
    }
  }

  private InvoiceProduct parseProductLine(String line) {
    try {

      String[] parts = line.split("\\s+");

      if (parts.length < 2) return null;

      String code = parts[0];
      String name = extractProductName(parts);
      int currentIndex = name.split("\\s+").length + 1;

      BigDecimal quantity = parseQuantity(parts, currentIndex);
      BigDecimal unitPrice = parseUnitPrice(parts, currentIndex + 1);

      return InvoiceProduct.builder()
          .code(code)
          .name(name)
          .quantity(quantity)
          .price(unitPrice)
          .unitType("kg")
          .build();
    } catch (Exception e) {
      throw new PurchaseException("Erro ao processar linha do produto: " + line, e);
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

  private BigDecimal parseQuantity(String[] parts, int index) {
    if (index < parts.length && parts[index].matches("\\d+([,.]\\d+)?")) {
      String quantityStr = parts[index].replace(",", ".");
      BigDecimal quantity = new BigDecimal(quantityStr);
      return quantity;
    }
    throw new PurchaseException("Quantidade inválida encontrada na linha do produto.");
  }

  private BigDecimal parseUnitPrice(String[] parts, int index) {
    if (index < parts.length && parts[index].matches("\\d+([,.]\\d+)?")) {
      String priceStr = parts[index].replace(",", ".");
      BigDecimal price = new BigDecimal(priceStr);
      return price;
    }
    throw new PurchaseException("Preço unitário inválido encontrado na linha do produto.");
  }
}
