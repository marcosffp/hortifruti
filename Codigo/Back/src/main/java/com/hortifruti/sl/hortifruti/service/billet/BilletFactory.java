package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.dto.billet.BilletRequest;
import com.hortifruti.sl.hortifruti.dto.billet.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.billet.Pagador;
import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BilletFactory {

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
      String[] addressParts = address.split(",");
      if (addressParts.length < 5) {
        throw new BilletException(
            "O endereço do cliente está incompleto. Formato esperado: 'Rua, Numero, Complemento"
                + " (opcional), Bairro, Cidade - UF, CEP: XXXXX-XXX'");
      }

      String rua = addressParts[0].trim();
      String numero = addressParts[1].trim();
      String complemento = "";

      if (addressParts.length == 6) {
        complemento = addressParts[2].trim();
      }

      String bairro = addressParts[addressParts.length - 3].trim();
      String cidadeUf = addressParts[addressParts.length - 2].trim();
      String cep = addressParts[addressParts.length - 1].trim();

      String cidade = cidadeUf;
      String uf = "";
      if (cidadeUf.contains("-")) {
        String[] cidadeUfParts = cidadeUf.split("-");
        cidade = cidadeUfParts[0].trim();
        uf = cidadeUfParts[1].trim();
      }

      if (cep.startsWith("CEP:")) {
        cep = cep.replace("CEP:", "").trim().replaceAll("[^0-9]", "");
      }

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

      // O Sicoob trunca silenciosamente (sem erro) os campos de endereço/bairro do pagador que
      // excedem o limite do boleto, cortando o texto no meio da palavra. Tentamos primeiro sem o
      // complemento — rua e número já identificam o endereço sozinhos — e, se mesmo assim não
      // couber, cortamos o restante até o limite (preferindo cortar num espaço em vez de no meio de
      // uma palavra, mas cortando de qualquer forma se necessário) em vez de bloquear a emissão.
      if (enderecoCompleto.length() > ENDERECO_MAX_LENGTH && !complemento.isEmpty()) {
        enderecoCompleto = rua + ", " + numero;
      }

      if (enderecoCompleto.length() > ENDERECO_MAX_LENGTH) {
        enderecoCompleto = truncateAteLimite(enderecoCompleto, ENDERECO_MAX_LENGTH);
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

  /**
   * Corta {@code text} em até {@code maxLength} caracteres, preferindo recuar até o último espaço
   * dentro do limite (evita cortar no meio de uma palavra) — mas corta no limite exato mesmo assim
   * se não houver um espaço razoavelmente próximo do fim.
   */
  private String truncateAteLimite(String text, int maxLength) {
    String truncated = text.substring(0, maxLength);
    int lastSpace = truncated.lastIndexOf(' ');
    if (lastSpace > maxLength * 0.6) {
      return truncated.substring(0, lastSpace).trim();
    }
    return truncated.trim();
  }
}
