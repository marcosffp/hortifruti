package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProductResponse;
import com.hortifruti.sl.hortifruti.exception.ClientException;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
import com.hortifruti.sl.hortifruti.exception.PurchaseException;
import com.hortifruti.sl.hortifruti.mapper.CombinedScoreMapper;
import com.hortifruti.sl.hortifruti.model.enumeration.Status;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.ProductGrouperRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CombinedScoreService {

  private final CombinedScoreRepository combinedScoreRepository;
  private final CombinedScoreMapper combinedScoreMapper;
  private final ClientRepository clientRepository;
  private final PurchaseRepository purchaseRepository;
  private final ProductGrouperService productGrouper;
  private final ProductGrouperRepository productGrouperRepository;

  public void cancelGrouping(Long id) {
    if (!combinedScoreRepository.existsById(id)) {
      throw new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado.");
    }
    CombinedScore savedEntity =
        combinedScoreRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado."));
    Client client =
        clientRepository
            .findById(savedEntity.getClientId())
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "Cliente com ID " + savedEntity.getClientId() + " não encontrado."));
    BigDecimal newTotal = client.getTotalPurchaseValue().subtract(savedEntity.getTotalValue());
    client.setTotalPurchaseValue(newTotal);
    clientRepository.save(client);
    combinedScoreRepository.deleteById(id);
  }

  public Page<CombinedScoreResponse> listGroupings(Long clientId, Pageable pageable) {
    Page<CombinedScore> groupings;

    if (clientId != null) {
      groupings = combinedScoreRepository.findByClientIdOrderByConfirmedAtDesc(clientId, pageable);
    } else {
      groupings = combinedScoreRepository.findAllByOrderByConfirmedAtDesc(pageable);
    }

    return groupings.map(combinedScoreMapper::toResponse);
  }

  @Transactional
  public void createCombinedScore(CombinedScoreRequest request) {
    // Validação de parâmetros obrigatórios
    if (request.clientId() == null) {
      throw new ClientException("ID do cliente não fornecido.");
    }

    if (request.startDate() == null || request.endDate() == null) {
      throw new PurchaseException("Período de consulta inválido: datas não podem ser nulas.");
    }

    if (request.endDate().isBefore(request.startDate())) {
      throw new PurchaseException("Data final não pode ser anterior à data inicial.");
    }

    // Busca do cliente
    Client client =
        clientRepository
            .findById(request.clientId())
            .orElseThrow(
                () ->
                    new ClientException(
                        "Cliente com ID " + request.clientId() + " não encontrado."));

    // Busca das compras no período especificado
    List<Purchase> purchases =
        purchaseRepository.findByClientIdAndPurchaseDateBetween(
            request.clientId(), request.startDate(), request.endDate());

    // Retorno vazio se não houver compras
    if (purchases.isEmpty()) {
      throw new PurchaseException(
          "Nenhuma compra encontrada para o cliente no período especificado.");
    }

    // Agrupamento de produtos
    List<GroupedProduct> groupedProducts =
        productGrouper.groupProducts(purchases, client.isVariablePrice());

    // Cálculo do valor total
    BigDecimal totalValue =
        groupedProducts.stream()
            .map(GroupedProduct::getTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Criação do CombinedScore
    CombinedScore combinedScore =
        CombinedScore.builder().clientId(request.clientId()).totalValue(totalValue).build();

    // Salva o CombinedScore e pega a entidade salva com ID (com flush para garantir o ID)
    CombinedScore savedCombinedScore = combinedScoreRepository.saveAndFlush(combinedScore);

    // Associação dos produtos agrupados ao CombinedScore salvo
    groupedProducts.forEach(product -> product.setCombinedScore(savedCombinedScore));

    // Salva os produtos agrupados em lote
    productGrouperRepository.saveAll(groupedProducts);
  }

  @Transactional
  public void confirmPayment(Long id) {
    // Verifica se o agrupamento existe
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado."));

    // Verifica se o agrupamento possui boleto
    if (combinedScore.isHasBillet()) {
      throw new CombinedScoreException(
          "Não é possível confirmar o pagamento de um agrupamento com boleto.");
    }

    if (combinedScore.getStatus() == Status.PAGO) {
      throw new CombinedScoreException("O pagamento deste agrupamento já foi confirmado.");
    }

    // Atualiza o status para pago
    combinedScore.setStatus(Status.PAGO);
    combinedScoreRepository.save(combinedScore);
  }

  @Transactional
  public void cancelPayment(Long id) {
    // Verifica se o agrupamento existe
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new CombinedScoreException("Agrupamento com o ID " + id + " não encontrado."));

    // Verifica se o agrupamento já está cancelado
    if (combinedScore.getStatus() == Status.CANCELADO) {
      throw new CombinedScoreException("O pagamento deste agrupamento já foi cancelado.");
    }

    // Verifica as condições para cancelamento
    if (combinedScore.isHasBillet() || combinedScore.isHasInvoice()) {
      throw new CombinedScoreException(
          "Não é possível cancelar o pagamento enquanto o boleto ou a nota fiscal não forem"
              + " resolvidos.");
    }

    // Atualiza o status para cancelado
    combinedScore.setStatus(Status.CANCELADO);
    combinedScoreRepository.save(combinedScore);
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

    // Converte para DTO e ordena alfabeticamente pelo nome
    return combinedScore.getGroupedProducts().stream()
        .sorted(Comparator.comparing(GroupedProduct::getName)) // Ordena pelo nome
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

  @Transactional
  public void updateStatusAfterBilletCancellation(String nossoNumero) {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findByNumber(nossoNumero)
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "CombinedScore com o número " + nossoNumero + " não encontrado."));

    if (combinedScore.isHasInvoice()) {
      combinedScore.setStatus(Status.CANCELADO_BOLETO);
    } else {
      combinedScore.setStatus(Status.CANCELADO);
    }

    combinedScoreRepository.save(combinedScore);
  }

  @Transactional
  public void updateStatusAfterInvoiceCancellation(String invoiceNumber) {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findByNumber(invoiceNumber)
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "CombinedScore com o número " + invoiceNumber + " não encontrado."));

    if (combinedScore.isHasBillet()) {
      combinedScore.setStatus(Status.CANCELADO_NOTA_FISCAL);
    } else {
      combinedScore.setStatus(Status.CANCELADO);
    }

    combinedScoreRepository.save(combinedScore);
  }
}
