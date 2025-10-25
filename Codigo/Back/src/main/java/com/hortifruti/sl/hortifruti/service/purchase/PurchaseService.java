package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseResponse;
import com.hortifruti.sl.hortifruti.exception.ClientException;
import com.hortifruti.sl.hortifruti.exception.PurchaseException;
import com.hortifruti.sl.hortifruti.mapper.InvoiceProductMapper;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

  @Transactional
  public Purchase processPurchaseFile(MultipartFile file) throws IOException {
    if (file == null) {
      throw new PurchaseException("Arquivo não fornecido");
    }

    Purchase purchase = purchaseProcessingService.processPurchaseFile(file);
    return purchase;
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
        .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName())) // Ordena pelo nome
        .map(invoiceProductMapper::toResponse)
        .toList();
  }

  @Transactional
  public void recalculateTotal(Long purchaseId) {
    // Busca a compra pelo ID
    Purchase purchase =
        purchaseRepository
            .findById(purchaseId)
            .orElseThrow(
                () -> new PurchaseException("Compra não encontrada com o ID: " + purchaseId));

    // Recalcula o total somando os valores dos produtos associados
    BigDecimal newTotal =
        purchase.getInvoiceProducts().stream()
            .map(
                invoiceProduct ->
                    invoiceProduct
                        .getPrice()
                        .multiply(BigDecimal.valueOf(invoiceProduct.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Atualiza o total da compra
    purchase.setTotal(newTotal);

    // Salva a compra atualizada no banco de dados
    purchaseRepository.save(purchase);
  }

  @Transactional(readOnly = true)
  public Page<PurchaseResponse> getPurchasesByDateRange(
      LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
    // Busca as compras no repositório dentro do intervalo de datas
    Page<Purchase> purchases =
        purchaseRepository.findByPurchaseDateBetweenOrderByPurchaseDateDesc(
            startDate, endDate, pageable);

    // Mapeia as compras para PurchaseResponse
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
    // Converte as datas recebidas como String para LocalDateTime
    LocalDateTime start = LocalDateTime.parse(startDate);
    LocalDateTime end = LocalDateTime.parse(endDate);

    // Configura a paginação
    Pageable pageable = PageRequest.of(page, size);

    // Busca as compras no repositório dentro do intervalo de datas
    Page<Purchase> purchases =
        purchaseRepository.findByPurchaseDateBetweenOrderByPurchaseDateDesc(start, end, pageable);

    // Mapeia as compras para PurchaseResponse
    return purchases.map(
        purchase ->
            new PurchaseResponse(
                purchase.getId(),
                purchase.getPurchaseDate(),
                purchase.getTotal(),
                purchase.getUpdatedAt()));
  }
}
