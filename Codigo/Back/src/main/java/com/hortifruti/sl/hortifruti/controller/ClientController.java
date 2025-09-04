package com.hortifruti.sl.hortifruti.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hortifruti.sl.hortifruti.dto.ClientRequest;
import com.hortifruti.sl.hortifruti.dto.ClientResponse;
import com.hortifruti.sl.hortifruti.service.ClientService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

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
        public ResponseEntity<List<ClientResponse>> getAllClients(){
            return ResponseEntity.ok(clientService.getAllClients());
        }
}
