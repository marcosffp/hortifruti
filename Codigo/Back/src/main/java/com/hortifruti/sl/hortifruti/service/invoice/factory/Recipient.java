package com.hortifruti.sl.hortifruti.service.invoice.factory;

import com.hortifruti.sl.hortifruti.dto.invoice.AddressRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.RecipientRequest;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Recipient {
  private final ClientRepository clientRepository;

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
    String zipCode = "";
    String cideCode = client.getCideCode().trim();

    try {
      if (cideCode.length() != 7) {
        throw new RuntimeException("Código IBGE inválido: " + cideCode);
      }

      String cleanAddress = address.replaceAll("\\r?\\n", " ").replaceAll("\\s+", " ").trim();

      String[] addressAndZip = cleanAddress.split(",?\\s*CEP:\\s*");
      String addressWithoutZip = addressAndZip[0].trim();

      if (addressAndZip.length > 1) {
        zipCode = addressAndZip[1].replaceAll("\\D", "");
      }

      String[] parts = addressWithoutZip.split("\\s*,\\s*");
      parts =
          java.util.Arrays.stream(parts)
              .map(String::trim)
              .filter(p -> !p.isEmpty())
              .toArray(String[]::new);

      if (parts.length >= 1) {
        street = truncateIfNeeded(parts[0], 60);
      }

      if (parts.length >= 2 && !parts[1].isBlank()) {
        number = parts[1];
      }

      Pattern ufPattern = Pattern.compile("-\\s*([A-Z]{2})(?:\\s*,|$)");
      Matcher ufMatcher = ufPattern.matcher(addressWithoutZip);

      if (ufMatcher.find()) {
        state = ufMatcher.group(1).toUpperCase();
      } else {
        throw new InvoiceException("UF não encontrada no endereço: " + address);
      }

      Pattern cityPattern = Pattern.compile("([^,]+)\\s*-\\s*" + state);
      Matcher cityMatcher = cityPattern.matcher(addressWithoutZip);

      if (cityMatcher.find()) {
        city = cityMatcher.group(1).trim();
      }
      if (parts.length >= 3) {
        neighborhood = parts[parts.length - 3];
      }

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
