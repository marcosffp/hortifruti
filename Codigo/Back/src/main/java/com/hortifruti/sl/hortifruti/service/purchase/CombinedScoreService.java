package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.invoice.OpenInvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseImageResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.WildcardBilletRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientLastGroupingResponse;
import com.hortifruti.sl.hortifruti.exception.purchase.ClientException;
import com.hortifruti.sl.hortifruti.exception.purchase.CombinedScoreException;
import com.hortifruti.sl.hortifruti.exception.purchase.PurchaseException;
import com.hortifruti.sl.hortifruti.mapper.CombinedScoreMapper;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.model.purchase.Status;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.GroupedProductRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CombinedScoreService {

  private static final String WILDCARD_PRODUCT_CODE = "113";
  private static final String WILDCARD_PRODUCT_NAME = "PRODUTO CORINGA";
  private static final BigDecimal WILDCARD_PRODUCT_PRICE = new BigDecimal("1.00");
  private static final int INVOICE_ONLY_DUE_DAYS = 20;

  private final CombinedScoreRepository combinedScoreRepository;
  private final CombinedScoreMapper combinedScoreMapper;
  private final ClientRepository clientRepository;
  private final PurchaseRepository purchaseRepository;
  private final GroupedProductService productGrouper;
  private final GroupedProductRepository productGrouperRepository;
  private final CombinedScorePhotoService combinedScorePhotoService;

  public Page<CombinedScoreResponse> listGroupings(Long clientId, Pageable pageable) {
    Page<CombinedScore> groupings;

    if (clientId != null) {
      groupings = combinedScoreRepository.findByClientIdOrderByIdDesc(clientId, pageable);
    } else {
      groupings = combinedScoreRepository.findAllByOrderByIdDesc(pageable);
    }

    return groupings.map(combinedScoreMapper::toResponse);
  }

  @Transactional
  public void createCombinedScore(CombinedScoreRequest request) {

    if (request.endDate().isBefore(request.startDate())) {
      throw new PurchaseException("Data final não pode ser anterior à data inicial.");
    }

    Client client =
        clientRepository
            .findById(request.clientId())
            .orElseThrow(
                () ->
                    new ClientException(
                        "Cliente com ID " + request.clientId() + " não encontrado."));

    List<Purchase> purchases =
        purchaseRepository.findByClientIdAndPurchaseDateBetween(
            request.clientId(), request.startDate(), request.endDate());

    if (purchases.isEmpty()) {
      throw new PurchaseException(
          "Nenhuma compra encontrada para o cliente no período especificado.");
    }

    List<GroupedProduct> groupedProducts =
        productGrouper.groupProducts(purchases, !client.isVariablePrice());

    BigDecimal totalValue =
        groupedProducts.stream()
            .map(GroupedProduct::getTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    CombinedScore combinedScore =
        CombinedScore.builder().clientId(request.clientId()).totalValue(totalValue).build();

    LocalDate confirmedDate =
        request.confirmedAt() != null
            ? request.confirmedAt()
            : ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate();
    combinedScore.setConfirmedAt(confirmedDate);
    combinedScore.setDueDate(DueDateCalculator.calculate(client, combinedScore.getConfirmedAt()));
    combinedScore.setStatus(Status.PENDENTE);
    combinedScore.setHasBillet(false);
    combinedScore.setHasInvoice(false);

    CombinedScore savedCombinedScore = combinedScoreRepository.saveAndFlush(combinedScore);

    groupedProducts.forEach(product -> product.setCombinedScore(savedCombinedScore));

    productGrouperRepository.saveAll(groupedProducts);

    // Vincula as compras de origem ao agrupamento — sem isso não há como depois descobrir quais
    // compras (e fotos de comprovante, se houver) compuseram este agrupamento específico.
    purchases.forEach(purchase -> purchase.setCombinedScoreId(savedCombinedScore.getId()));
    purchaseRepository.saveAll(purchases);

    // Junta as fotos de comprovante (se houver) num único PDF para download — ver
    // CombinedScorePhotoService.
    combinedScorePhotoService.generatePhotosPdfIfNeeded(savedCombinedScore.getId());
  }

  @Transactional
  public Long createWildcardCombinedScore(WildcardBilletRequest request) {
    Client client =
        clientRepository
            .findById(request.clientId())
            .orElseThrow(
                () ->
                    new ClientException(
                        "Cliente com ID " + request.clientId() + " não encontrado."));

    if (!client.isOnlyBillet()) {
      throw new ClientException("Cliente não está configurado para boleto avulso.");
    }

    LocalDate confirmedDate = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate();

    CombinedScore combinedScore =
        CombinedScore.builder().clientId(request.clientId()).totalValue(request.value()).build();
    combinedScore.setConfirmedAt(confirmedDate);
    combinedScore.setDueDate(DueDateCalculator.calculate(client, confirmedDate));
    combinedScore.setStatus(Status.PENDENTE);
    combinedScore.setHasBillet(false);
    combinedScore.setHasInvoice(false);

    CombinedScore savedCombinedScore = combinedScoreRepository.saveAndFlush(combinedScore);

    GroupedProduct wildcardProduct =
        GroupedProduct.builder()
            .code(WILDCARD_PRODUCT_CODE)
            .name(WILDCARD_PRODUCT_NAME)
            .price(WILDCARD_PRODUCT_PRICE)
            .quantity(request.value())
            .totalValue(request.value())
            .combinedScore(savedCombinedScore)
            .build();

    productGrouperRepository.save(wildcardProduct);

    return savedCombinedScore.getId();
  }

  @Transactional
  public void confirmPayment(Long id) {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado."));

    if (combinedScore.isHasBillet()) {
      throw new CombinedScoreException(
          "Não é possível confirmar o pagamento de um agrupamento com boleto.");
    }

    if (combinedScore.getStatus() == Status.PAGO) {
      throw new CombinedScoreException("O pagamento deste agrupamento já foi confirmado.");
    }

    combinedScore.setStatus(Status.PAGO);
    combinedScoreRepository.save(combinedScore);
  }

  @Transactional
  public void cancelPayment(Long id) {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado."));

    if (combinedScore.getStatus() == Status.CANCELADO) {
      throw new CombinedScoreException("O pagamento deste agrupamento já foi cancelado.");
    }

    if (combinedScore.isHasBillet() || combinedScore.isHasInvoice()) {
      throw new CombinedScoreException(
          "Não é possível cancelar o pagamento enquanto o boleto ou a nota fiscal não forem"
              + " resolvidos.");
    }

    combinedScore.setStatus(Status.CANCELADO);
    combinedScoreRepository.save(combinedScore);
  }

  @Transactional(readOnly = true)
  public List<ClientLastGroupingResponse> getLastGroupingPerClient() {
    return combinedScoreRepository.findLastGroupingPerClient().stream()
        .map(
            cs ->
                new ClientLastGroupingResponse(
                    cs.getClientId(), cs.getConfirmedAt(), cs.getTotalValue()))
        .toList();
  }

  /** Compras deste agrupamento que têm foto de comprovante anexada — ver {@link Purchase}. */
  @Transactional(readOnly = true)
  public List<PurchaseImageResponse> listImagesByCombinedScoreId(Long combinedScoreId) {
    return purchaseRepository.findByCombinedScoreIdAndImagemR2KeyIsNotNull(combinedScoreId).stream()
        .map(p -> new PurchaseImageResponse(p.getId(), p.getPurchaseDate(), p.getTotal()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<GroupedProductResponse> getGroupedProductsByCombinedScoreId(Long combinedScoreId) {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(combinedScoreId)
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "Agrupamento com o ID " + combinedScoreId + " não encontrado."));

    return combinedScore.getGroupedProducts().stream()
        .sorted(Comparator.comparing(GroupedProduct::getName))
        .map(
            product ->
                new GroupedProductResponse(
                    product.getCode(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantity(),
                    product.getTotalValue()))
        .toList();
  }

  /**
   * yourNumber (seuNumero) não é único entre agrupamentos (ver {@link
   * CombinedScoreRepository#findAllByYourNumber}) — retorna o ID do agrupamento mais recente (maior
   * ID) para esse número, ou {@code null} se nenhum for encontrado.
   */
  public Long findLatestIdByYourNumber(String yourNumber) {
    return combinedScoreRepository.findAllByYourNumber(yourNumber).stream()
        .max(Comparator.comparing(CombinedScore::getId))
        .map(CombinedScore::getId)
        .orElse(null);
  }

  @Transactional
  public void updateStatusAfterBilletCancellation(String nossoNumero) {
    List<CombinedScore> combinedScores = combinedScoreRepository.findAllByYourNumber(nossoNumero);
    if (combinedScores.isEmpty()) {
      throw new CombinedScoreException(
          "CombinedScore com o número " + nossoNumero + " não encontrado.");
    }

    for (CombinedScore combinedScore : combinedScores) {
      if (combinedScore.isHasInvoice()) {
        combinedScore.setStatus(Status.CANCELADO_BOLETO);
      } else {
        combinedScore.setStatus(Status.CANCELADO);
      }
      combinedScore.setHasBillet(false);
    }

    combinedScoreRepository.saveAll(combinedScores);
  }

  /**
   * Reverte {@code hasInvoice}/{@code invoiceRef} quando a Sefaz rejeita a nota (status
   * erro/denegado/cancelado) — sem isso o agrupamento fica com {@code hasInvoice=true} para sempre,
   * mostrando "Ver NF" no front mesmo a nota nunca tendo sido autorizada. Diferente de {@link
   * #updateStatusAfterInvoiceCancellation}, não mexe no {@code Status} do agrupamento: a compra em
   * si continua válida, só a tentativa de emissão falhou, então o agrupamento deve poder tentar
   * gerar a NF de novo. Best-effort: se o agrupamento não existir mais (já deletado), não há o que
   * reverter.
   */
  @Transactional
  public void clearInvoiceAfterRejection(String ref) {
    combinedScoreRepository
        .findByInvoiceRef(ref)
        .ifPresent(
            combinedScore -> {
              combinedScore.setHasInvoice(false);
              combinedScore.setInvoiceRef(null);
              combinedScoreRepository.save(combinedScore);
            });
  }

  @Transactional
  public void updateStatusAfterInvoiceCancellation(String ref) {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findByInvoiceRef(ref)
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "CombinedScore com o identificação " + ref + " não encontrado."));

    if (combinedScore.isHasBillet()) {
      combinedScore.setStatus(Status.CANCELADO_NOTA_FISCAL);
    } else {
      combinedScore.setStatus(Status.CANCELADO);
    }
    combinedScore.setHasInvoice(false);

    combinedScoreRepository.save(combinedScore);
  }

  @Transactional
  public void recalculateTotal(Long combinedScoreId) {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(combinedScoreId)
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "Agrupamento com o ID " + combinedScoreId + " não encontrado."));

    BigDecimal newTotal =
        combinedScore.getGroupedProducts().stream()
            .map(GroupedProduct::getTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    combinedScore.setTotalValue(newTotal);

    combinedScoreRepository.save(combinedScore);
  }

  @Transactional(readOnly = true)
  public List<CombinedScore> getCombinedScoresWithInvoice(LocalDate startDate, LocalDate endDate) {
    return combinedScoreRepository.findByHasInvoiceTrueAndConfirmedAtBetween(startDate, endDate);
  }

  /**
   * Busca todos os agrupamentos cuja data de CONFIRMAÇÃO (confirmedAt) está dentro de {@code
   * [startDate, endDate]}. Usado pelo dashboard para as consultas de fluxo de vendas baseadas em
   * confirmação (em vez de vencimento/dueDate).
   */
  @Transactional(readOnly = true)
  public List<CombinedScore> findAllConfirmedBetween(LocalDate startDate, LocalDate endDate) {
    return combinedScoreRepository.findByConfirmedAtBetween(startDate, endDate);
  }

  /**
   * Lista agrupamentos que só têm nota fiscal emitida (sem boleto), ainda pendentes de confirmação
   * manual de pagamento. Como não há boleto para confirmar o status junto ao Sicoob, o vencimento
   * exibido é sempre a data de emissão da NF acrescida de {@value #INVOICE_ONLY_DUE_DAYS} dias,
   * independentemente do dueDate calculado na criação do agrupamento.
   */
  public List<CombinedScore> findAllPendingWithBilletByClient(Long clientId) {
    return combinedScoreRepository.findAllPendingWithBilletByClient(clientId, Status.PENDENTE);
  }

  public List<CombinedScore> findAllPendingByClient(Long clientId) {
    return combinedScoreRepository.findAllPendingByClient(clientId, Status.PENDENTE);
  }

  public List<CombinedScore> findAllOpenBillets() {
    return combinedScoreRepository.findAllOpenBillets(Status.PENDENTE);
  }

  public Optional<CombinedScore> findByInvoiceRef(String invoiceRef) {
    return combinedScoreRepository.findByInvoiceRef(invoiceRef);
  }

  /** Agrupamentos com nota fiscal emitida, usado para localizar a ref pelo número da NF. */
  public List<CombinedScore> findAllWithInvoiceRef() {
    return combinedScoreRepository.findAll().stream()
        .filter(
            cs -> cs.isHasInvoice() && cs.getInvoiceRef() != null && !cs.getInvoiceRef().isEmpty())
        .toList();
  }

  public CombinedScore findById(Long id) {
    return combinedScoreRepository
        .findById(id)
        .orElseThrow(
            () -> new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado."));
  }

  /**
   * Busca o agrupamento travando a linha (SELECT ... FOR UPDATE) até o fim da transação do
   * chamador. Deve ser usado logo no início de fluxos de emissão de boleto/NF para evitar que duas
   * requisições concorrentes (ex: duplo clique) leiam {@code hasBillet}/{@code hasInvoice} como
   * falso ao mesmo tempo e gerem o documento em duplicidade.
   */
  @Transactional
  public CombinedScore findByIdForUpdate(Long id) {
    return combinedScoreRepository
        .findByIdForUpdate(id)
        .orElseThrow(
            () -> new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado."));
  }

  /**
   * Busca agrupamentos pelo "nosso número" do Sicoob. Usado pelo cancelamento manual/avulso de
   * boleto, quando o operador só possui o número do boleto (sem saber, ou sem existir, um
   * CombinedScore local vinculado).
   */
  public List<CombinedScore> findAllByOurNumberSicoob(String ourNumber) {
    return combinedScoreRepository.findAllByOurNumberSicoob(ourNumber);
  }

  /**
   * Atualiza o status de um agrupamento. Ponto único de escrita de status usado por outros domínios
   * (ex: billet) para não precisarem acessar {@link CombinedScoreRepository} diretamente.
   */
  @Transactional
  public CombinedScore updateStatus(Long id, Status status) {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado."));
    combinedScore.setStatus(status);
    return combinedScoreRepository.save(combinedScore);
  }

  @Transactional
  public CombinedScore save(CombinedScore combinedScore) {
    return combinedScoreRepository.save(combinedScore);
  }

  @Transactional(readOnly = true)
  public List<OpenInvoiceResponse> listOpenInvoiceOnlyScores() {
    List<CombinedScore> scores = combinedScoreRepository.findAllOpenInvoiceOnly(Status.PENDENTE);
    if (scores.isEmpty()) {
      return List.of();
    }

    List<Long> clientIds = scores.stream().map(CombinedScore::getClientId).distinct().toList();
    Map<Long, String> clientNamesById =
        clientRepository.findAllById(clientIds).stream()
            .collect(Collectors.toMap(Client::getId, Client::getClientName));

    return scores.stream()
        .map(
            cs ->
                new OpenInvoiceResponse(
                    cs.getId(),
                    cs.getClientId(),
                    clientNamesById.getOrDefault(cs.getClientId(), "Cliente não encontrado"),
                    cs.getTotalValue(),
                    cs.getConfirmedAt(),
                    cs.getConfirmedAt() == null
                        ? null
                        : cs.getConfirmedAt().plusDays(INVOICE_ONLY_DUE_DAYS),
                    cs.getInvoiceRef()))
        .sorted(
            Comparator.comparing(
                OpenInvoiceResponse::dueDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }
}
