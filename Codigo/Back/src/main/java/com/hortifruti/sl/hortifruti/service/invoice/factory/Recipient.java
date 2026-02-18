package com.hortifruti.sl.hortifruti.service.invoice.factory;

import com.hortifruti.sl.hortifruti.dto.invoice.AddressRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.RecipientRequest;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Recipient {
  private final ClientRepository clientRepository;

  //private final String CIDE_CODE = "3157807";
  private final String COUNTRY_CODE = "1058";
  private final String COUNTRY_NAME = "Brazil";

  public RecipientRequest createRecipientRequest(Long clientId) {
    Client client =
        clientRepository
            .findById(clientId)
            .orElseThrow(
                () -> new InvoiceException("Cliente com id " + clientId + " não encontrado"));

    AddressRequest addressDto = parseAddress(client.getAddress(), client);

    // Remove formatação do documento (pontos, traços, barras)
    String documentoLimpo = client.getDocument().replaceAll("[^0-9]", "");

    // Valida se o documento está presente
    if (documentoLimpo.isEmpty()) {
      throw new InvoiceException("Cliente não possui CPF ou CNPJ cadastrado");
    }

    return new RecipientRequest(
        documentoLimpo.length() == 14 ? documentoLimpo : null,
        documentoLimpo.length() == 11 ? documentoLimpo : null,
        client.getClientName(),
        null,
        client.getPhoneNumber(),
        client.getEmail(),
        addressDto,
        client.getStateRegistration(),
        client.getStateIndicator());
  }

private AddressRequest parseAddress(String address, Client client) {
  String street = "Rua não informada";
  String number = "N/A";
  String neighborhood = "Bairro não informado";
  String city = null;
  String state = null;
  String zipCode = "";

  try {
    System.out.println("=== parseAddress ===");
    System.out.println("Address RAW:\n" + address);

    // Extrai CEP
    String[] addressAndZip = address.split(",?\\s*CEP:\\s*");
    String addressWithoutZip = addressAndZip[0].trim();

    if (addressAndZip.length > 1) {
      zipCode = addressAndZip[1].trim().replaceAll("\\D", "");
    }

    System.out.println("CEP extraído: " + zipCode);

    String[] parts = addressWithoutZip.split("\\s*,\\s*");

    System.out.println("Parts do endereço:");
    for (int i = 0; i < parts.length; i++) {
      System.out.println("parts[" + i + "] = " + parts[i]);
    }

    if (parts.length > 0) {
      street = truncateIfNeeded(parts[0].trim(), 60);
    }
    if (parts.length > 1) {
      number = parts[1].trim();
    }
    if (parts.length > 2) {
      neighborhood = parts[2].trim();
    }

    // 🔥 AQUI ESTÁ A CORREÇÃO REAL
    for (String part : parts) {
      if (part.contains("-")) {
        String[] cityAndStateParts = part.split("\\s*-\\s*");
        if (cityAndStateParts.length == 2) {
          city = cityAndStateParts[0].trim();
          state = cityAndStateParts[1].trim().toUpperCase();
          break;
        }
      }
    }

    if (city == null || state == null) {
      throw new InvoiceException("Cidade/UF não detectadas no endereço: " + address);
    }

  } catch (Exception e) {
    throw new InvoiceException("Erro ao analisar o endereço do cliente: " + address, e);
  }

  System.out.println("=== RESULTADO FINAL ===");
  System.out.println("Street: " + street);
  System.out.println("Number: " + number);
  System.out.println("Neighborhood: " + neighborhood);
  System.out.println("City: " + city);
  System.out.println("State: " + state);
  System.out.println("CEP: " + zipCode);
  System.out.println("======================");

  return new AddressRequest(
      street,
      number,
      null,
      neighborhood,
      city,
      state,
      zipCode,
      client.getCideCode(),
      COUNTRY_CODE,
      COUNTRY_NAME
  );
}

private String truncateIfNeeded(String value, int maxLength) {
  return value.length() > maxLength ? value.substring(0, maxLength) : value;
}

}
