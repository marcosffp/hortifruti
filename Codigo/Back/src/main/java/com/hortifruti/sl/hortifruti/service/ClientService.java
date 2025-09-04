package com.hortifruti.sl.hortifruti.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.dto.ClientRequest;
import com.hortifruti.sl.hortifruti.dto.ClientResponse;
import com.hortifruti.sl.hortifruti.mapper.ClientMapper;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public Map<String, ClientResponse> saveClient(ClientRequest clientRequest) {
        Client client = clientMapper.toClient(clientRequest);
        Client savedClient = clientRepository.save(client);
        ClientResponse clientResponse = clientMapper.toClientResponse(savedClient);

        return Map.of("client", clientResponse);
    }

    public List<ClientResponse> getAllClients() {
        return clientRepository.findAll().stream().map(clientMapper::toClientResponse).toList();
    }

    public ClientResponse getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return clientMapper.toClientResponse(client);
    }

    public ClientResponse updateClient(Long id, ClientRequest clientRequest) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        existingClient.setClientName(clientRequest.clientName());
        existingClient.setEmail(clientRequest.email());
        existingClient.setPhoneNumber(clientRequest.phoneNumber());
        existingClient.setAddress(clientRequest.address());

        Client updatedClient = clientRepository.save(existingClient);
        return clientMapper.toClientResponse(updatedClient);
    }

    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return clientMapper.toClientResponse(client);
    }

    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new RuntimeException("Cliente não encontrado");
        }
        clientRepository.deleteById(id);
    }
}
