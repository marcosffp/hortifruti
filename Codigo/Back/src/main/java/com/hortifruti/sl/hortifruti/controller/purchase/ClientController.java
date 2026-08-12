package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientSelectionInfo;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientSummary;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientWithLastPurchaseResponse;
import com.hortifruti.sl.hortifruti.service.purchase.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/clients")
@AllArgsConstructor
public class ClientController {

  private final ClientService clientService;

  @PostMapping("/register")
  public ResponseEntity<Map<String, ClientResponse>> registerClient(
      @Valid @RequestBody ClientRequest clientRequest) {
    return ResponseEntity.ok(clientService.saveClient(clientRequest));
  }

  @GetMapping
  public ResponseEntity<List<ClientResponse>> getAllClients() {
    return ResponseEntity.ok(clientService.getAllClients());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id) {
    return ResponseEntity.ok(clientService.getClientById(id));
  }

  @GetMapping("/name/{name}")
  public ResponseEntity<ClientResponse> getClienteByName(@PathVariable String name) {
    return ResponseEntity.ok(clientService.getClientByNameClient(name));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ClientResponse> updateClient(
      @PathVariable Long id, @Valid @RequestBody ClientRequest clientRequest) {
    return ResponseEntity.ok(clientService.updateClient(id, clientRequest));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
    clientService.deleteClient(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/with-last-purchase")
  public ResponseEntity<Page<ClientWithLastPurchaseResponse>> getClientsWithLastPurchase(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(clientService.getClientsWithLastPurchase(pageable));
  }

  @GetMapping("/{id}/summary")
  public ClientSummary getClientSummary(@PathVariable Long id) {
    return clientService.getClientSummary(id);
  }

  @GetMapping("/for-selection")
  public ResponseEntity<List<ClientSelectionInfo>> getAllClientsForSelection() {
    List<ClientSelectionInfo> clients = clientService.getAllClientsForSelection();
    return ResponseEntity.ok(clients);
  }
}
