package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientSelectionInfo;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientSummary;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientWithLastPurchaseResponse;
import com.hortifruti.sl.hortifruti.exception.purchase.ClientException;
import com.hortifruti.sl.hortifruti.exception.purchase.PurchaseException;
import com.hortifruti.sl.hortifruti.mapper.ClientMapper;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientPurchaseTotal;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.PurchaseRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ClientService {
  private final ClientRepository clientRepository;
  private final ClientMapper clientMapper;
  private final PurchaseRepository purchaseRepository;

  public Map<String, ClientResponse> saveClient(ClientRequest clientRequest) {
    Client client = clientMapper.toClient(clientRequest);
    client.setEmail(blankToNull(client.getEmail()));
    client.setPhoneNumber(blankToNull(client.getPhoneNumber()));
    Client savedClient = clientRepository.save(client);
    // Cliente recém-criado não tem compras ainda.
    ClientResponse clientResponse =
        withTotalPurchaseValue(clientMapper.toClientResponse(savedClient), BigDecimal.ZERO);
    return Map.of("client", clientResponse);
  }

  // E-mail é único no banco; string vazia é um valor "de verdade" pra essa
  // constraint (diferente de NULL), então normalizamos pra NULL aqui pra
  // permitir vários clientes sem e-mail cadastrado.
  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }

  // "Valor Total em Compras" é sempre a soma real das compras do cliente, calculada aqui
  // em vez de mantida como um contador incrementado/decrementado em outros serviços — esse
  // contador dessincronizava (ex.: chegava a ficar negativo) porque nem toda confirmação de
  // pagamento passava pelo mesmo caminho que o cancelamento.
  private ClientResponse withTotalPurchaseValue(ClientResponse response, BigDecimal total) {
    return new ClientResponse(
        response.id(),
        response.clientName(),
        response.email(),
        response.phoneNumber(),
        response.address(),
        response.document(),
        response.variablePrice(),
        response.stateRegistration(),
        response.stateIndicator(),
        response.cideCode(),
        response.onlyBillet(),
        response.requiresPurchaseProof(),
        response.lastPurchaseDate(),
        total);
  }

  public List<ClientResponse> getAllClients() {
    List<Client> clients = clientRepository.findAll();

    Map<Long, BigDecimal> totalsByClientId =
        purchaseRepository.sumTotalGroupedByClientId().stream()
            .collect(Collectors.toMap(ClientPurchaseTotal::clientId, ClientPurchaseTotal::total));

    return clients.stream()
        .map(
            client ->
                withTotalPurchaseValue(
                    clientMapper.toClientResponse(client),
                    totalsByClientId.getOrDefault(client.getId(), BigDecimal.ZERO)))
        .toList();
  }

  public ClientResponse getClientByNameClient(String nameClient) {
    Client client =
        clientRepository
            .findByClientName(nameClient)
            .orElseThrow(() -> new ClientException("Cliente não encontrado"));
    return withTotalPurchaseValue(
        clientMapper.toClientResponse(client),
        purchaseRepository.sumTotalByClientId(client.getId()));
  }

  public ClientResponse updateClient(Long id, ClientRequest clientRequest) {
    Client existingClient =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ClientException("Cliente não encontrado"));

    existingClient.setClientName(clientRequest.clientName());
    existingClient.setEmail(blankToNull(clientRequest.email()));
    existingClient.setPhoneNumber(blankToNull(clientRequest.phoneNumber()));
    existingClient.setAddress(clientRequest.address());
    existingClient.setDocument(clientRequest.document());
    existingClient.setVariablePrice(clientRequest.variablePrice());
    existingClient.setOnlyBillet(clientRequest.onlyBillet());
    existingClient.setRequiresPurchaseProof(clientRequest.requiresPurchaseProof());
    existingClient.setStateRegistration(clientRequest.stateRegistration());
    existingClient.setStateIndicator(clientRequest.stateIndicator());

    if (clientRequest.cideCode() == null) {
      existingClient.setCideCode("");
    } else {
      existingClient.setCideCode(clientRequest.cideCode());
    }

    Client updatedClient = clientRepository.save(existingClient);
    return withTotalPurchaseValue(
        clientMapper.toClientResponse(updatedClient), purchaseRepository.sumTotalByClientId(id));
  }

  public ClientResponse getClientById(Long id) {
    Client client =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ClientException("Cliente não encontrado"));
    return withTotalPurchaseValue(
        clientMapper.toClientResponse(client), purchaseRepository.sumTotalByClientId(id));
  }

  public void deleteClient(Long id) {
    if (!clientRepository.existsById(id)) {
      throw new ClientException("Cliente não encontrado");
    }
    clientRepository.deleteById(id);
  }

  public Client findMatchingClient(String clientName) {
    String cleanInputName = clientName.toUpperCase().trim();
    String inputFirstName = cleanInputName.split("\\s+")[0];

    List<Client> clients = clientRepository.findAll();

    Optional<Client> exactMatch =
        clients.stream()
            .filter(client -> client.getClientName().toUpperCase().trim().equals(cleanInputName))
            .findFirst();

    if (exactMatch.isPresent()) {
      return exactMatch.get();
    }

    Optional<Client> firstNameMatch =
        clients.stream()
            .filter(
                client -> {
                  String clientFirstName =
                      client.getClientName().split("\\s+")[0].toUpperCase().trim();
                  return clientFirstName.equals(inputFirstName)
                      || clientFirstName.replace("L", "").equals(inputFirstName.replace("L", ""));
                })
            .findFirst();

    if (firstNameMatch.isEmpty()) {
      throw new PurchaseException("Cliente não encontrado: " + clientName);
    }

    return firstNameMatch.get();
  }

  /**
   * O filtro "só clientes com compra" acontece depois da paginação (não dá pra expressar isso como
   * parte da query de {@code findAll(Pageable)} sem uma query dedicada) — por isso o conteúdo de
   * uma página pode vir com menos itens que {@code pageable.getPageSize()}, mas {@code
   * totalElements}/{@code totalPages} refletem o total de clientes cadastrados, não só os com
   * compra.
   */
  public Page<ClientWithLastPurchaseResponse> getClientsWithLastPurchase(Pageable pageable) {
    Page<Client> clientPage = clientRepository.findAll(pageable);
    List<ClientWithLastPurchaseResponse> content =
        clientPage.getContent().stream()
            .map(
                client -> {
                  Optional<Purchase> lastPurchase =
                      purchaseRepository.findTopByClientIdOrderByPurchaseDateDesc(client.getId());
                  return new ClientWithLastPurchaseResponse(
                      client.getId(),
                      client.getClientName(),
                      lastPurchase.map(Purchase::getPurchaseDate).orElse(null),
                      lastPurchase.map(Purchase::getTotal).orElse(null));
                })
            .filter(response -> response.lastPurchaseDate() != null)
            .toList();
    return new PageImpl<>(content, pageable, clientPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public ClientSummary getClientSummary(Long id) {
    Client client =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ClientException("Cliente não encontrado"));

    List<Purchase> purchases = purchaseRepository.findByClientId(id);

    int totalProducts =
        purchases.stream().mapToInt(purchase -> purchase.getInvoiceProducts().size()).sum();

    double totalValue =
        purchases.stream().mapToDouble(purchase -> purchase.getTotal().doubleValue()).sum();

    return new ClientSummary(
        client.getClientName(), client.getAddress(), totalProducts, totalValue);
  }

  public List<ClientSelectionInfo> getAllClientsForSelection() {
    return clientRepository.findAll().stream()
        .map(client -> new ClientSelectionInfo(client.getId(), client.getClientName()))
        .toList();
  }

  public Map<Long, String> getClientNamesByIds(List<Long> ids) {
    return clientRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Client::getId, Client::getClientName));
  }

  public Optional<String> findClientName(Long id) {
    return clientRepository.findById(id).map(Client::getClientName);
  }

  public Client findById(Long id) {
    return clientRepository
        .findById(id)
        .orElseThrow(() -> new ClientException("Cliente não encontrado"));
  }

  public Optional<Client> findByDocument(String document) {
    return clientRepository.findByDocument(document);
  }
}
