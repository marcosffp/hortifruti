package com.hortifruti.sl.hortifruti.service.invoice.factory;

import com.hortifruti.sl.hortifruti.dto.invoice.AddressRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.RecipientRequest;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.service.purchase.ClientAddressParser;
import com.hortifruti.sl.hortifruti.service.purchase.ClientAddressParser.ParsedAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Recipient {
  private final ClientRepository clientRepository;
  private final ClientAddressParser clientAddressParser;

  private final String COUNTRY_CODE = "1058";
  private final String COUNTRY_NAME = "Brazil";

  public RecipientRequest createRecipientRequest(Long clientId) {
    Client client =
        clientRepository
            .findById(clientId)
            .orElseThrow(
                () -> new InvoiceException("Cliente com id " + clientId + " não encontrado"));

    AddressRequest addressDto = parseAddress(client.getAddress(), client);

    // CNPJ passa a aceitar letras (A-Z) a partir de ago/2026 — remove só a máscara
    // (pontos/traço/barra), preservando eventuais letras. CPF continua só numérico.
    String documentoLimpo = client.getDocument().replaceAll("[^0-9A-Za-z]", "").toUpperCase();

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
    String number = "S/N";
    String neighborhood = "Bairro não informado";
    String city = "Cidade não informada";
    String state;
    String zipCode;
    String cideCode = client.getCideCode().trim();

    try {
      if (cideCode.length() != 7) {
        throw new RuntimeException("Código IBGE inválido: " + cideCode);
      }

      ParsedAddress parsed = clientAddressParser.parse(address);
      zipCode = parsed.zipCode();

      if (!parsed.street().isBlank()) {
        street = truncateIfNeeded(parsed.street(), 60);
      }
      if (!parsed.number().isBlank()) {
        number = parsed.number();
      }
      if (!parsed.neighborhood().isBlank()) {
        neighborhood = parsed.neighborhood();
      }
      if (!parsed.city().isBlank()) {
        city = parsed.city();
      }

      if (parsed.state().isBlank()) {
        throw new InvoiceException("UF não encontrada no endereço: " + address);
      }
      state = parsed.state();

      // Regra especial: cliente APTA sempre é faturado como SP, independente do endereço cadastrado
      String firstName = client.getClientName().split("\\s+")[0].toUpperCase();
      if (firstName.contains("APTA")) {
        state = "SP";
      }

      if (!state.matches("[A-Z]{2}")) {
        throw new InvoiceException("UF inválida após parse: " + state);
      }

    } catch (Exception e) {
      throw new InvoiceException(
          "Erro ao analisar o endereço do cliente: " + address + " | Detalhes: " + e.getMessage(),
          e);
    }

    return new AddressRequest(
        street,
        number,
        null,
        neighborhood,
        city,
        state,
        zipCode,
        cideCode,
        COUNTRY_CODE,
        COUNTRY_NAME);
  }

  private String truncateIfNeeded(String value, int maxLength) {
    return value.length() > maxLength ? value.substring(0, maxLength) : value;
  }
}
