package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.dto.billet.BilletRequest;
import com.hortifruti.sl.hortifruti.dto.billet.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.billet.Pagador;
import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.purchase.ClientAddressParser;
import com.hortifruti.sl.hortifruti.service.purchase.ClientAddressParser.ParsedAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BilletFactory {

  private final ClientAddressParser clientAddressParser;

  @Value("${sicoob.num.cliente}")
  private Integer clientNumber;

  @Value("${sicoob.num.conta.corrente}")
  private Integer accountNumber;

  private Integer MODALITY_CODE = 1;
  private String DOCUMENT_SPECIES_CODE = "DM";
  private Integer BOLETO_ISSUANCE_IDENTIFICATION = 1;
  private Integer BOLETO_DISTRIBUTION_IDENTIFICATION = 1;
  private Integer DISCOUNT_TYPE = 0;
  private Integer FINE_TYPE = 0;
  private Integer INTEREST_TYPE = 3;
  private Integer INSTALLMENT_NUMBER = 1;
  private Boolean GENERATE_PDF = true;

  private static final int ENDERECO_MAX_LENGTH = 40;
  private static final int BAIRRO_MAX_LENGTH = 30;

  public BilletRequest createCompleteBoletoRequest(BilletRequestSimplified boletoSimplificado) {
    return new BilletRequest(
        clientNumber, // numeroCliente
        MODALITY_CODE, // codigoModalidade
        accountNumber, // numeroContaCorrente
        DOCUMENT_SPECIES_CODE, // codigoEspecieDocumento
        boletoSimplificado.dataEmissao(),
        boletoSimplificado.seuNumero(),
        BOLETO_ISSUANCE_IDENTIFICATION, // identificacaoEmissaoBoleto
        BOLETO_DISTRIBUTION_IDENTIFICATION, // identificacaoDistribuicaoBoleto
        boletoSimplificado.valor(),
        boletoSimplificado.dataVencimento(),
        DISCOUNT_TYPE, // tipoDesconto
        FINE_TYPE, // tipoMulta
        INTEREST_TYPE, // tipoJurosMora
        INSTALLMENT_NUMBER, // numeroParcela
        boletoSimplificado.pagador(),
        GENERATE_PDF // gerarPdf
        );
  }

  public BilletRequestSimplified createBilletRequest(
      CombinedScore combinedScore, Long combinedScoreId, Pagador pagador, String number) {
    return new BilletRequestSimplified(
        combinedScore.getConfirmedAt().toString(),
        number,
        combinedScore.getTotalValue(),
        combinedScore.getDueDate().toString(),
        pagador);
  }

  public Pagador createPagadorFromClient(Client client) {
    String address = client.getAddress();

    try {
      ParsedAddress parsedAddress = clientAddressParser.parse(address);

      String rua = parsedAddress.street();
      String numero = parsedAddress.number();
      String complemento = parsedAddress.complement();
      String bairro = parsedAddress.neighborhood();
      String cidade = parsedAddress.city();
      String uf = parsedAddress.state();
      String cep = parsedAddress.zipCode();

      if (rua.isEmpty()) {
        throw new BilletException("Rua não pode estar vazia no endereço do cliente.");
      }
      if (numero.isEmpty()) {
        throw new BilletException("Número não pode estar vazio no endereço do cliente.");
      }
      if (bairro.isEmpty()) {
        throw new BilletException("Bairro não pode estar vazio no endereço do cliente.");
      }
      if (cidade.isEmpty()) {
        throw new BilletException("Cidade não pode estar vazia no endereço do cliente.");
      }
      if (uf.isEmpty()) {
        throw new BilletException("UF não pode estar vazia no endereço do cliente.");
      }
      if (cep.length() != 8) {
        throw new BilletException("CEP deve conter exatamente 8 dígitos numéricos.");
      }

      String enderecoCompleto =
          rua + ", " + numero + (complemento.isEmpty() ? "" : ", " + complemento);

      // O Sicoob trunca silenciosamente (sem erro) os campos de endereço/bairro do
      // pagador que excedem o limite do boleto, cortando o texto no meio da palavra.
      // Validamos aqui para avisar o usuário em vez de gerar um boleto com endereço cortado.
      if (enderecoCompleto.length() > ENDERECO_MAX_LENGTH) {
        throw new BilletException(
            "Endereço do cliente muito longo para o boleto (máximo "
                + ENDERECO_MAX_LENGTH
                + " caracteres, incluindo rua, número e complemento). Reduza o complemento ou"
                + " o endereço do cliente.");
      }
      // O cadastro de clientes já limita o bairro a 30 caracteres, mas clientes
      // cadastrados antes dessa limitação podem ter um bairro mais longo salvo.
      // Trunca em vez de falhar a emissão do boleto.
      if (bairro.length() > BAIRRO_MAX_LENGTH) {
        bairro = bairro.substring(0, BAIRRO_MAX_LENGTH).trim();
      }

      return new Pagador(
          // CNPJ passa a aceitar letras (A-Z) a partir de ago/2026 — remove só a máscara,
          // preservando eventuais letras. CPF continua só numérico.
          client.getDocument().replaceAll("[^0-9A-Za-z]", "").toUpperCase(),
          client.getClientName(),
          enderecoCompleto,
          bairro,
          cidade,
          cep,
          uf);

    } catch (Exception e) {
      throw new BilletException(
          "Erro ao processar endereço do cliente: "
              + e.getMessage()
              + ". Endereço recebido: '"
              + address
              + "'",
          e);
    }
  }
}
