package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.client.ClientRequest;
import com.hortifruti.sl.hortifruti.dto.client.ClientResponse;
import com.hortifruti.sl.hortifruti.dto.client.ClientSelectionInfo;
import com.hortifruti.sl.hortifruti.dto.client.ClientSummary;
import com.hortifruti.sl.hortifruti.dto.client.ClientWithLastPurchaseResponse;
import com.hortifruti.sl.hortifruti.exception.ClientException;
import com.hortifruti.sl.hortifruti.exception.PurchaseException;
import com.hortifruti.sl.hortifruti.mapper.ClientMapper;
import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.model.Purchase;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.PurchaseRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
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
    Client savedClient = clientRepository.save(client);
    ClientResponse clientResponse = clientMapper.toClientResponse(savedClient);
    return Map.of("client", clientResponse);
  }

  public List<ClientResponse> getAllClients() {
    return clientRepository.findAll().stream().map(clientMapper::toClientResponse).toList();
  }

  public ClientResponse getClientByNameClient(String nameClient) {
    Client client =
        clientRepository
            .findByClientName(nameClient)
            .orElseThrow(() -> new ClientException("Cliente não encontrado"));
    return clientMapper.toClientResponse(client);
  }

  public ClientResponse updateClient(Long id, ClientRequest clientRequest) {
    Client existingClient =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ClientException("Cliente não encontrado"));

    existingClient.setClientName(clientRequest.clientName());
    existingClient.setEmail(clientRequest.email());
    existingClient.setPhoneNumber(clientRequest.phoneNumber());
    existingClient.setAddress(clientRequest.address());
    existingClient.setDocument(clientRequest.document());

    Client updatedClient = clientRepository.save(existingClient);
    return clientMapper.toClientResponse(updatedClient);
  }

  public ClientResponse getClientById(Long id) {
    Client client =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ClientException("Cliente não encontrado"));
    return clientMapper.toClientResponse(client);
  }

  public void deleteClient(Long id) {
    if (!clientRepository.existsById(id)) {
      throw new ClientException("Cliente não encontrado");
    }
    clientRepository.deleteById(id);
  }

  public Client findMatchingClient(String clientName) {
    String firstName = clientName.split("\\s+")[0].toUpperCase().trim();
    List<Client> clients = clientRepository.findAll();

    Optional<Client> clientOptional =
        clients.stream()
            .filter(
                client -> {
                  String clientFirstName =
                      client.getClientName().split("\\s+")[0].toUpperCase().trim();
                  return clientFirstName.equals(firstName)
                      || clientFirstName.replace("L", "").equals(firstName.replace("L", ""))
                      || clientFirstName.contains(firstName)
                      || firstName.contains(clientFirstName);
                })
            .findFirst();

    if (clientOptional.isEmpty()) {
      throw new PurchaseException("Cliente não encontrado: " + clientName);
    }

    return clientOptional.get();
  }

  public List<ClientWithLastPurchaseResponse> getClientsWithLastPurchase() {
    return clientRepository.findAll().stream()
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
        .filter(
            response ->
                response.lastPurchaseDate() != null) // Filtra clientes sem data de última compra
        .toList();
  }

  @Transactional(readOnly = true)
  public ClientSummary getClientSummary(Long id) {
      Client client = clientRepository.findById(id)
              .orElseThrow(() -> new ClientException("Cliente não encontrado"));

      List<Purchase> purchases = purchaseRepository.findByClientId(id);

      int totalProducts = purchases.stream()
              .mapToInt(purchase -> purchase.getInvoiceProducts().size())
              .sum();

      double totalValue = purchases.stream()
              .mapToDouble(purchase -> purchase.getTotal().doubleValue())
              .sum();

      return new ClientSummary(client.getClientName(), client.getAddress(), totalProducts, totalValue);
  }

  public List<ClientSelectionInfo> getAllClientsForSelection() {
    return clientRepository.findAll().stream()
        .map(client -> new ClientSelectionInfo(client.getId(), client.getClientName()))
        .toList();
  }
}
