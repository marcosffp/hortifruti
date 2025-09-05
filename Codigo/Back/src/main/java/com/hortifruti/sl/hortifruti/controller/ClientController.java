package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.ClientRequest;
import com.hortifruti.sl.hortifruti.dto.ClientResponse;
import com.hortifruti.sl.hortifruti.service.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
public class ClientController {

  @Autowired private ClientService clientService;

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

  @GetMapping("/email/{email}")
  public ResponseEntity<ClientResponse> getClientByEmail(@PathVariable String email) {
    return ResponseEntity.ok(clientService.getClientByEmail(email));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ClientResponse> updateClient(
      @PathVariable Long id, @Valid @RequestBody ClientRequest clientRequest) {
    return ResponseEntity.ok(clientService.updateClient(id, clientRequest));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
    clientService.deleteClient(id);
    return ResponseEntity.noContent().build();
  }
}
