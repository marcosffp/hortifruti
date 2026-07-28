package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.ManualPurchaseItemRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.ManualPurchaseRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseResponse;
import com.hortifruti.sl.hortifruti.exception.purchase.ClientException;
import com.hortifruti.sl.hortifruti.exception.purchase.PurchaseException;
import com.hortifruti.sl.hortifruti.mapper.InvoiceProductMapper;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.InvoiceProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class PurchaseService {

  private final PurchaseProcessingService purchaseProcessingService;
  private final PurchaseRepository purchaseRepository;
  private final ClientRepository clientRepository;
  private final InvoiceProductMapper invoiceProductMapper;
  private final InvoiceProductRepository invoiceProductRepository;
  private final FiscalProductRepository fiscalProductRepository;

  @Transactional
  public Purchase processPurchaseFile(MultipartFile file) throws IOException {
    if (file == null) {
      throw new PurchaseException("Arquivo não fornecido");
    }

    Purchase purchase = purchaseProcessingService.processPurchaseFile(file);
    Client client =
        clientRepository
            .findById(purchase.getClient().getId())
            .orElseThrow(
                () ->
                    new ClientException(
                        "Cliente não encontrado com o ID: " + purchase.getClient().getId()));
    client.setLastPurchaseDate(purchase.getCreatedAt().toLocalDate());
    clientRepository.save(client);
    return purchase;
  }

  @Transactional
  public Purchase createManualPurchase(ManualPurchaseRequest request) {
    if (request.items() == null || request.items().isEmpty()) {
      throw new PurchaseException("A compra precisa ter ao menos um item.");
    }

    Client client =
        clientRepository
            .findById(request.clientId())
            .orElseThrow(
                () -> new ClientException("Cliente não encontrado com o ID: " + request.clientId()));

    List<InvoiceProduct> invoiceProducts = new ArrayList<>();
    for (ManualPurchaseItemRequest item : request.items()) {
      if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
        throw new PurchaseException("A quantidade do item deve ser maior que zero.");
      }
      if (item.price() == null || item.price().compareTo(BigDecimal.ZERO) < 0) {
        throw new PurchaseException("O preço do item não pode ser negativo.");
      }

      FiscalProduct fiscalProduct =
          fiscalProductRepository
              .findByCode(item.code())
              .orElseThrow(
                  () ->
                      new PurchaseException(
                          "Produto não encontrado no catálogo para o código: " + item.code()));

      invoiceProducts.add(
          InvoiceProduct.builder()
              .code(fiscalProduct.getCode())
              .name(fiscalProduct.getDescription())
              .unitType(fiscalProduct.getUnidadeComercial())
              .price(item.price())
              .quantity(item.quantity())
              .build());
    }

    BigDecimal total =
        invoiceProducts.stream()
            .map(product -> product.getPrice().multiply(product.getQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (total.compareTo(BigDecimal.ZERO) <= 0) {
      throw new PurchaseException("O total da compra não pode ser zero ou negativo.");
    }

    Purchase purchase =
        Purchase.builder()
            .client(client)
            .purchaseDate(request.purchaseDate().atStartOfDay())
            .total(total)
            .build();

    purchase = purchaseRepository.save(purchase);

    List<InvoiceProduct> savedProducts = new ArrayList<>();
    for (InvoiceProduct product : invoiceProducts) {
      product.setPurchase(purchase);
      savedProducts.add(invoiceProductRepository.save(product));
    }

    purchase.setInvoiceProducts(savedProducts);
    purchase = purchaseRepository.save(purchase);

    client.setLastPurchaseDate(purchase.getCreatedAt().toLocalDate());
    clientRepository.save(client);

    return purchase;
  }

  @Transactional
  public InvoiceProductResponse addInvoiceProduct(Long purchaseId, ManualPurchaseItemRequest item) {
    Purchase purchase =
        purchaseRepository
            .findById(purchaseId)
            .orElseThrow(
                () -> new PurchaseException("Compra não encontrada com o ID: " + purchaseId));

    if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
      throw new PurchaseException("A quantidade do item deve ser maior que zero.");
    }
    if (item.price() == null || item.price().compareTo(BigDecimal.ZERO) < 0) {
      throw new PurchaseException("O preço do item não pode ser negativo.");
    }

    FiscalProduct fiscalProduct =
        fiscalProductRepository
            .findByCode(item.code())
            .orElseThrow(
                () ->
                    new PurchaseException(
                        "Produto não encontrado no catálogo para o código: " + item.code()));

    InvoiceProduct invoiceProduct =
        InvoiceProduct.builder()
            .code(fiscalProduct.getCode())
            .name(fiscalProduct.getDescription())
            .unitType(fiscalProduct.getUnidadeComercial())
            .price(item.price())
            .quantity(item.quantity())
            .purchase(purchase)
            .build();

    invoiceProduct = invoiceProductRepository.save(invoiceProduct);
    recalculateTotal(purchaseId);

    return invoiceProductMapper.toResponse(invoiceProduct);
  }

  public void deletePurchaseById(Long id) {
    Purchase purchase =
        purchaseRepository
            .findById(id)
            .orElseThrow(() -> new PurchaseException("Compra não encontrada com o ID: " + id));
    purchaseRepository.delete(purchase);
  }

  @Transactional(readOnly = true)
  public Page<PurchaseResponse> getPurchasesByClientOrdered(Long clientId, Pageable pageable) {
    clientRepository
        .findById(clientId)
        .orElseThrow(() -> new ClientException("Cliente não encontrado com o ID: " + clientId));

    return purchaseRepository
        .findByClientIdOrderByCreatedAtDesc(clientId, pageable)
        .map(
            purchase ->
                new PurchaseResponse(
                    purchase.getId(),
                    purchase.getPurchaseDate(),
                    purchase.getTotal(),
                    purchase.getUpdatedAt()));
  }

  @Transactional(readOnly = true)
  public List<InvoiceProductResponse> getInvoiceProductsByPurchaseId(Long purchaseId) {
    Purchase purchase =
        purchaseRepository
            .findById(purchaseId)
            .orElseThrow(
                () -> new PurchaseException("Compra não encontrada com o ID: " + purchaseId));

    return purchase.getInvoiceProducts().stream()
        .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))
        .map(invoiceProductMapper::toResponse)
        .toList();
  }

  @Transactional
  public void recalculateTotal(Long purchaseId) {
    Purchase purchase =
        purchaseRepository
            .findById(purchaseId)
            .orElseThrow(
                () -> new PurchaseException("Compra não encontrada com o ID: " + purchaseId));

    BigDecimal newTotal =
        purchase.getInvoiceProducts().stream()
            .map(invoiceProduct -> invoiceProduct.getPrice().multiply(invoiceProduct.getQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    purchase.setTotal(newTotal);

    purchaseRepository.save(purchase);
  }

  @Transactional(readOnly = true)
  public Page<PurchaseResponse> getPurchasesByDateRange(
      LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
    Page<Purchase> purchases =
        purchaseRepository.findByPurchaseDateBetweenOrderByPurchaseDateDesc(
            startDate, endDate, pageable);

    return purchases.map(
        purchase ->
            new PurchaseResponse(
                purchase.getId(),
                purchase.getPurchaseDate(),
                purchase.getTotal(),
                purchase.getUpdatedAt()));
  }

  @Transactional(readOnly = true)
  public Page<PurchaseResponse> getPurchasesByDateRange(
      String startDate, String endDate, int page, int size) {
    LocalDateTime start = LocalDateTime.parse(startDate);
    LocalDateTime end = LocalDateTime.parse(endDate);

    Pageable pageable = PageRequest.of(page, size);

    Page<Purchase> purchases =
        purchaseRepository.findByPurchaseDateBetweenOrderByPurchaseDateDesc(start, end, pageable);

    return purchases.map(
        purchase ->
            new PurchaseResponse(
                purchase.getId(),
                purchase.getPurchaseDate(),
                purchase.getTotal(),
                purchase.getUpdatedAt()));
  }
}
