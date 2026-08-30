package com.hortifruti.sl.hortifruti.service.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hortifruti.sl.hortifruti.dto.purchase.ManualPurchaseItemRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.ManualPurchaseRequest;
import com.hortifruti.sl.hortifruti.mapper.InvoiceProductMapper;
import com.hortifruti.sl.hortifruti.mapper.PurchaseMapper;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.InvoiceProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import com.hortifruti.sl.hortifruti.service.purchase.tabelapreco.NotaPrecoOficialChecker;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cobre só o comportamento novo de {@link PurchaseService#createManualPurchase}: o preço persistido
 * deve ser sobrescrito pelo preço oficial da tabela de preços do cliente quando ela existir e
 * divergir do preço informado — esse é o backstop server-side da regra "a tabela é autoritativa"
 * (ver {@code NotaPrecoOficialChecker}), independente do que a tela de revisão tenha mostrado.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

  @Mock private PurchaseProcessingService purchaseProcessingService;
  @Mock private PurchaseRepository purchaseRepository;
  @Mock private ClientRepository clientRepository;
  @Mock private InvoiceProductMapper invoiceProductMapper;
  @Mock private PurchaseMapper purchaseMapper;
  @Mock private InvoiceProductRepository invoiceProductRepository;
  @Mock private FiscalProductRepository fiscalProductRepository;
  @Mock private R2StorageService r2StorageService;
  @Mock private NotaPrecoOficialChecker notaPrecoOficialChecker;

  private PurchaseService service;

  @BeforeEach
  void setUp() {
    service =
        new PurchaseService(
            purchaseProcessingService,
            purchaseRepository,
            clientRepository,
            invoiceProductMapper,
            purchaseMapper,
            invoiceProductRepository,
            fiscalProductRepository,
            r2StorageService,
            notaPrecoOficialChecker);

    when(purchaseRepository.save(any(Purchase.class)))
        .thenAnswer(
            invocation -> {
              Purchase purchase = invocation.getArgument(0);
              if (purchase.getCreatedAt() == null) {
                purchase.setCreatedAt(LocalDateTime.now());
              }
              return purchase;
            });
    when(invoiceProductRepository.save(any(InvoiceProduct.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private Client cliente() {
    Client client = new Client();
    client.setId(1L);
    client.setClientName("LLINEA");
    return client;
  }

  private FiscalProduct produto() {
    FiscalProduct produto = new FiscalProduct();
    produto.setId(5L);
    produto.setCode("480");
    produto.setDescription("COUVE KG");
    produto.setUnidadeComercial("KG");
    return produto;
  }

  @Test
  void semTabelaOficialUsaOPrecoInformado() {
    when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente()));
    when(fiscalProductRepository.findByCode("480")).thenReturn(Optional.of(produto()));
    when(notaPrecoOficialChecker.precoOficial(1L, LocalDate.of(2026, 9, 15), 5L))
        .thenReturn(Optional.empty());

    ManualPurchaseRequest request =
        new ManualPurchaseRequest(
            1L,
            LocalDate.of(2026, 9, 15),
            List.of(
                new ManualPurchaseItemRequest(
                    "480", new BigDecimal("2"), new BigDecimal("10.00"))));

    Purchase purchase = service.createManualPurchase(request);

    ArgumentCaptor<InvoiceProduct> capturado = ArgumentCaptor.forClass(InvoiceProduct.class);
    org.mockito.Mockito.verify(invoiceProductRepository).save(capturado.capture());
    assertThat(capturado.getValue().getPrice()).isEqualByComparingTo("10.00");
    assertThat(purchase.getTotal()).isEqualByComparingTo("20.00");
  }

  @Test
  void comTabelaOficialDivergenteSobrescreveOPrecoInformado() {
    when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente()));
    when(fiscalProductRepository.findByCode("480")).thenReturn(Optional.of(produto()));
    when(notaPrecoOficialChecker.precoOficial(1L, LocalDate.of(2026, 9, 15), 5L))
        .thenReturn(Optional.of(new BigDecimal("12.99")));

    ManualPurchaseRequest request =
        new ManualPurchaseRequest(
            1L,
            LocalDate.of(2026, 9, 15),
            List.of(
                new ManualPurchaseItemRequest(
                    "480", new BigDecimal("2"), new BigDecimal("10.00"))));

    Purchase purchase = service.createManualPurchase(request);

    ArgumentCaptor<InvoiceProduct> capturado = ArgumentCaptor.forClass(InvoiceProduct.class);
    org.mockito.Mockito.verify(invoiceProductRepository).save(capturado.capture());
    assertThat(capturado.getValue().getPrice()).isEqualByComparingTo("12.99");
    assertThat(purchase.getTotal()).isEqualByComparingTo("25.98");
  }

  @Test
  void comTabelaOficialIgualAoInformadoNaoAlteraNada() {
    when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente()));
    when(fiscalProductRepository.findByCode("480")).thenReturn(Optional.of(produto()));
    when(notaPrecoOficialChecker.precoOficial(1L, LocalDate.of(2026, 9, 15), 5L))
        .thenReturn(Optional.of(new BigDecimal("10.00")));

    ManualPurchaseRequest request =
        new ManualPurchaseRequest(
            1L,
            LocalDate.of(2026, 9, 15),
            List.of(
                new ManualPurchaseItemRequest(
                    "480", new BigDecimal("2"), new BigDecimal("10.00"))));

    service.createManualPurchase(request);

    ArgumentCaptor<InvoiceProduct> capturado = ArgumentCaptor.forClass(InvoiceProduct.class);
    org.mockito.Mockito.verify(invoiceProductRepository).save(capturado.capture());
    assertThat(capturado.getValue().getPrice()).isEqualByComparingTo("10.00");
  }
}
