package com.hortifruti.sl.hortifruti.service.purchase;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Parser único do endereço em texto livre do cadastro de cliente ({@code Client.address}),
 * formato documentado: {@code "Rua, Numero, Complemento (opcional), Bairro, Cidade - UF, CEP:
 * XXXXX-XXX"}. Compartilhado entre a emissão de NF-e ({@code
 * com.hortifruti.sl.hortifruti.service.invoice.factory.Recipient}) e a geração de boleto ({@code
 * BilletFactory}) — antes cada um reimplementava a própria lógica de corte, com regras
 * ligeiramente diferentes (posições fixas vs. regex), então o mesmo cadastro podia sair com
 * endereço correto num fluxo e errado no outro.
 *
 * <p>Este parser só extrai o que consegue identificar pela posição/marcadores do texto — não
 * valida obrigatoriedade de campo nem aplica limite de tamanho. Cada chamador decide como reagir a
 * um campo ausente (usar um valor padrão vs. lançar erro) e qual limite de tamanho aplicar, porque
 * essas regras são específicas do destino (NF-e vs. boleto), não do parsing em si.
 */
@Component
public class ClientAddressParser {

  public record ParsedAddress(
      String street,
      String number,
      String complement,
      String neighborhood,
      String city,
      String state,
      String zipCode) {}

  private static final Pattern ZIP_MARKER =
      Pattern.compile(",?\\s*CEP:\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern STATE_SUFFIX = Pattern.compile("-\\s*([A-Za-z]{2})\\s*$");

  public ParsedAddress parse(String rawAddress) {
    String clean = rawAddress.replaceAll("\\r?\\n", " ").replaceAll("\\s+", " ").trim();

    Matcher zipMatcher = ZIP_MARKER.matcher(clean);
    String withoutZip;
    String zipCode;
    if (zipMatcher.find()) {
      withoutZip = clean.substring(0, zipMatcher.start()).trim();
      zipCode = clean.substring(zipMatcher.end()).replaceAll("\\D", "");
    } else {
      withoutZip = clean;
      zipCode = "";
    }

    Matcher stateMatcher = STATE_SUFFIX.matcher(withoutZip);
    String state = "";
    String withoutState = withoutZip;
    if (stateMatcher.find()) {
      state = stateMatcher.group(1).toUpperCase();
      withoutState = withoutZip.substring(0, stateMatcher.start()).trim();
      if (withoutState.endsWith(",")) {
        withoutState = withoutState.substring(0, withoutState.length() - 1).trim();
      }
    }

    // Sem filtrar strings vazias: um campo em branco (ex.: número não informado) ainda ocupa sua
    // posição na lista como elemento "" — descartá-lo (como uma versão anterior fazia via
    // .filter(p -> !p.isEmpty())) desalinha todos os campos seguintes um passo à esquerda (ex.: o
    // bairro passa a ser lido como número), corrompendo o endereço inteiro em vez de só deixar o
    // campo em branco vazio.
    String[] parts =
        Arrays.stream(withoutState.split("\\s*,\\s*")).map(String::trim).toArray(String[]::new);

    // Layout de "parts" (CEP e UF já removidos): [rua, numero, (complemento,) bairro, cidade].
    // "cidade" e "bairro" são contados a partir do FIM (últimos 2 elementos) porque o complemento
    // é opcional e só desloca os elementos do meio — contar a partir do início quebraria sempre
    // que o complemento estivesse ausente.
    String street = parts.length > 0 ? parts[0] : "";
    String number = parts.length > 1 ? parts[1] : "";
    String city = parts.length > 0 ? parts[parts.length - 1] : "";
    String neighborhood = parts.length > 2 ? parts[parts.length - 2] : "";
    String complement = parts.length > 4 ? parts[2] : "";

    return new ParsedAddress(street, number, complement, neighborhood, city, state, zipCode);
  }
}
