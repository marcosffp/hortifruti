package com.hortifruti.sl.hortifruti.service.invoice.factory;

import org.springframework.stereotype.Component;

import com.hortifruti.sl.hortifruti.dto.invoice.AddressRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.RecipientRequest;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Recipient {
    private final ClientRepository clientRepository;

    private final String CIDE_CODE = "3157807";
    private final String COUNTRY_CODE = "1058";
    private final String COUNTRY_NAME = "Brazil";

    public RecipientRequest createRecipientRequest(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new InvoiceException("Cliente com id " + clientId + " não encontrado"));

        AddressRequest addressDto = parseAddress(client.getAddress());

        return new RecipientRequest(
                client.getDocument().length() == 14 ? client.getDocument() : null,
                client.getDocument().length() == 11 ? client.getDocument() : null,
                client.getClientName(),
                null,
                client.getPhoneNumber(),
                client.getEmail(),
                addressDto,
                client.getStateRegistration(),
                client.getStateIndicator()
        );
    }

private AddressRequest parseAddress(String address) {
    String street = "Rua não informada";
    String number = "N/A";
    String neighborhood = "Bairro não informado";
    String city = "Cidade não informada";
    String state = "MG";
    String zipCode = "";

    try {
        // Extrai o CEP primeiro
        String[] addressAndZip = address.split(",?\\s*CEP:\\s*");
        String addressWithoutZip = addressAndZip[0].trim();
        if (addressAndZip.length > 1) {
            zipCode = addressAndZip[1].trim().replaceAll("\\D", "");
        }

        String[] parts = addressWithoutZip.split("\\s*,\\s*");

        if (parts.length >= 1) {
            // Primeira parte: Rua
            street = parts[0].trim();
            street = truncateIfNeeded(street, 60);
        }

        if (parts.length >= 2) {
            // Segunda parte: Número
            number = parts[1].trim();
        }

        if (parts.length >= 3) {
            // Terceira parte: Bairro
            neighborhood = parts[2].trim();
        }

        if (parts.length >= 4) {
            // Quarta parte: Cidade - Estado
            String cityAndState = parts[3].trim();
            String[] cityAndStateParts = cityAndState.split("\\s*-\\s*");
            if (cityAndStateParts.length >= 1) {
                city = cityAndStateParts[0].trim();
            }
            if (cityAndStateParts.length >= 2) {
                state = cityAndStateParts[1].trim().toUpperCase();
            }
        }

    } catch (Exception e) {
        throw new InvoiceException("Erro ao analisar o endereço do cliente: " + address, e);
    }

    return new AddressRequest(
            street,
            number,
            null,
            neighborhood,
            city,
            state,
            zipCode,
            CIDE_CODE,
            COUNTRY_CODE,
            COUNTRY_NAME
    );
}

    private String truncateIfNeeded(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
